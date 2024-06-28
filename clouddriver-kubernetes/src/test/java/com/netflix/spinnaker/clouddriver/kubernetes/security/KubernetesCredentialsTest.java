/*
 * Copyright 2020 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.kubernetes.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonSyntaxException;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.clouddriver.data.task.DefaultTask;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesAccountProperties.ManagedAccount;
import com.netflix.spinnaker.clouddriver.kubernetes.description.AccountResourcePropertyRegistry;
import com.netflix.spinnaker.clouddriver.kubernetes.description.GlobalResourcePropertyRegistry;
import com.netflix.spinnaker.clouddriver.kubernetes.description.KubernetesSpinnakerKindMap;
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesKind;
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesManifest;
import com.netflix.spinnaker.clouddriver.kubernetes.names.KubernetesManifestNamer;
import com.netflix.spinnaker.clouddriver.kubernetes.names.KubernetesNamerRegistry;
import com.netflix.spinnaker.clouddriver.kubernetes.op.handler.KubernetesUnregisteredCustomResourceHandler;
import com.netflix.spinnaker.clouddriver.kubernetes.op.job.KubectlJobExecutor;
import com.netflix.spinnaker.clouddriver.kubernetes.op.job.KubectlJobExecutor.KubectlException;
import com.netflix.spinnaker.clouddriver.kubernetes.op.job.KubectlJobExecutor.KubectlNotFoundException;
import com.netflix.spinnaker.kork.configserver.CloudConfigResourceService;
import com.netflix.spinnaker.kork.configserver.ConfigFileService;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

final class KubernetesCredentialsTest {
  private static final String ACCOUNT_NAME = "my-account";
  private static final String DEPLOYMENT_NAME = "my-deployment";
  private static final String NAMESPACE = "my-namespace";
  private final String OP_NAME = "KubernetesCredentialsTest";
  private final Task task = new DefaultTask("task-id");

  private KubernetesCredentials getCredentials(Registry registry, KubectlJobExecutor jobExecutor) {
    KubernetesCredentials.Factory factory =
        new KubernetesCredentials.Factory(
            registry,
            new KubernetesNamerRegistry(ImmutableList.of(new KubernetesManifestNamer())),
            jobExecutor,
            new ConfigFileService(new CloudConfigResourceService()),
            new AccountResourcePropertyRegistry.Factory(
                new GlobalResourcePropertyRegistry(
                    ImmutableList.of(), new KubernetesUnregisteredCustomResourceHandler())),
            new KubernetesKindRegistry.Factory(
                new GlobalKubernetesKindRegistry(ImmutableList.of())),
            new KubernetesSpinnakerKindMap(ImmutableList.of()),
            new GlobalResourcePropertyRegistry(
                ImmutableList.of(), new KubernetesUnregisteredCustomResourceHandler()));
    ManagedAccount managedAccount = new ManagedAccount();
    managedAccount.setName("my-account");
    return factory.build(managedAccount);
  }

  private KubernetesManifest getManifest() {
    KubernetesManifest manifest = new KubernetesManifest();
    manifest.put("metadata", new HashMap<>());
    manifest.setName(DEPLOYMENT_NAME);
    manifest.setNamespace(NAMESPACE);
    manifest.setKind(KubernetesKind.DEPLOYMENT);
    return manifest;
  }

  @Test
  void returnValueForSuccessfulList() {
    KubectlJobExecutor jobExecutor = mock(KubectlJobExecutor.class);
    Registry registry = new DefaultRegistry();
    KubernetesCredentials credentials = getCredentials(registry, jobExecutor);

    KubernetesManifest manifest = getManifest();
    when(jobExecutor.list(eq(credentials), any(), any(), any()))
        .thenReturn(ImmutableList.of(manifest));
    ImmutableList<KubernetesManifest> result =
        credentials.list(
            ImmutableList.of(KubernetesKind.DEPLOYMENT, KubernetesKind.REPLICA_SET), NAMESPACE);
    assertThat(result).containsExactly(manifest);
  }

  @Test
  void propagatedExceptionForListThrowingKubectlException() {
    KubectlJobExecutor jobExecutor = mock(KubectlJobExecutor.class);
    Registry registry = new DefaultRegistry();
    KubernetesCredentials credentials = getCredentials(registry, jobExecutor);

    KubectlException exception =
        new KubectlException(
            "Failed to parse kubectl output: failure", new JsonSyntaxException("failure"));
    when(jobExecutor.list(eq(credentials), any(), any(), any())).thenThrow(exception);

    // Assert that a KubectlException is passed through without modification
    assertThatThrownBy(
            () ->
                credentials.list(
                    ImmutableList.of(KubernetesKind.DEPLOYMENT, KubernetesKind.REPLICA_SET),
                    NAMESPACE))
        .isEqualTo(exception);
  }

  @Test
  void propagatedExceptionForListThrowingOtherException() {
    KubectlJobExecutor jobExecutor = mock(KubectlJobExecutor.class);
    Registry registry = new DefaultRegistry();
    KubernetesCredentials credentials = getCredentials(registry, jobExecutor);

    Exception cause = new CustomException("Kubernetes error");
    when(jobExecutor.list(eq(credentials), any(), any(), any())).thenThrow(cause);

    // Assert that the source exception is wrapped in a KubectlException with details about the call
    // that failed
    assertThatThrownBy(
            () ->
                credentials.list(
                    ImmutableList.of(KubernetesKind.DEPLOYMENT, KubernetesKind.REPLICA_SET),
                    NAMESPACE))
        .isEqualTo(cause);
  }

  @Test
  void replaceWhenResourceExists() {
    KubernetesManifest manifest = getManifest();
    KubectlJobExecutor jobExecutor = mock(KubectlJobExecutor.class);
    KubernetesCredentials credentials = getCredentials(new NoopRegistry(), jobExecutor);
    when(jobExecutor.create(credentials, manifest, task, OP_NAME))
        .thenThrow(new KubectlException("Create failed: Error from server (AlreadyExists)"));
    when(jobExecutor.replace(credentials, manifest, task, OP_NAME)).thenReturn(manifest);

    KubernetesManifest result = credentials.createOrReplace(getManifest(), task, OP_NAME);
    assertThat(result).isEqualTo(manifest);
  }

  @Test
  void replaceWhenResourceDoesNotExist() {
    KubernetesManifest manifest = getManifest();
    KubectlJobExecutor jobExecutor = mock(KubectlJobExecutor.class);
    KubernetesCredentials credentials = getCredentials(new NoopRegistry(), jobExecutor);
    when(jobExecutor.replace(credentials, manifest, task, OP_NAME))
        .thenThrow(new KubectlNotFoundException("Not found"));
    when(jobExecutor.create(credentials, manifest, task, OP_NAME)).thenReturn(manifest);

    KubernetesManifest result = credentials.createOrReplace(getManifest(), task, OP_NAME);
    assertThat(result).isEqualTo(manifest);
  }

  // This is an error type that will only ever be thrown by stubs in this test; that way we can
  // assert that it is thrown and be sure that we aren't accidentally passing due to an unrelated
  // exception.
  private static class CustomException extends RuntimeException {
    CustomException(String message) {
      super(message);
    }
  }
}
