package userauth.controller;

import userauth.client.network.RemoteApiClient;
import userauth.common.RemoteAction;
import userauth.common.RemoteResponse;

import java.util.List;
import java.util.Map;

abstract class RemoteControllerSupport {
    protected RemoteResponse request(RemoteApiClient remoteApiClient, RemoteAction action, Object... arguments) {
        return remoteApiClient.send(action, arguments);
    }

    protected String requestString(RemoteApiClient remoteApiClient, RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = request(remoteApiClient, action, arguments);
            if (!response.isSuccess()) {
                return response.getMessage();
            }
            String payload = response.payloadAsString();
            return payload == null || payload.isBlank() ? "SUCCESS" : payload;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> requestList(RemoteApiClient remoteApiClient, RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = request(remoteApiClient, action, arguments);
            if (!response.isSuccess()) {
                return List.of();
            }
            Object payload = response.getPayload();
            return payload instanceof List<?> values ? (List<T>) values : List.of();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    protected <T> T requestPayload(RemoteApiClient remoteApiClient, Class<T> type, RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = request(remoteApiClient, action, arguments);
            return response.isSuccess() ? response.payloadAs(type) : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected <K, V> Map<K, V> requestMap(RemoteApiClient remoteApiClient, RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = request(remoteApiClient, action, arguments);
            if (!response.isSuccess()) {
                return Map.of();
            }
            Object payload = response.getPayload();
            return payload instanceof Map<?, ?> map ? (Map<K, V>) map : Map.of();
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    protected double requestDouble(RemoteApiClient remoteApiClient, RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = request(remoteApiClient, action, arguments);
            if (!response.isSuccess()) {
                return 0.0;
            }
            Object payload = response.getPayload();
            return payload instanceof Double value ? value : 0.0;
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }
}
