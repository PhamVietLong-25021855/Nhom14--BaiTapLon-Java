package userauth.controller;

import userauth.client.network.RemoteApiClient;
import userauth.common.RemoteAction;
import userauth.common.RemoteResponse;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;
import userauth.service.AutobidService;

import java.util.List;

public class AutobidController {
    private final AutobidService autobidService;
    private final RemoteApiClient remoteApiClient;

    public AutobidController(AutobidService autobidService) {
        this.autobidService = autobidService;
        this.remoteApiClient = null;
    }

    public AutobidController(RemoteApiClient remoteApiClient) {
        this.autobidService = null;
        this.remoteApiClient = remoteApiClient;
    }

    public String createAutobid(int bidderId, int auctionId, double maxPrice, double increment) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTOBID_CREATE, bidderId, auctionId, maxPrice, increment);
        }

        try {
            autobidService.createAutobid(bidderId, auctionId, maxPrice, increment);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public String updateAutobid(int bidderId, int id, double maxPrice, double increment) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTOBID_UPDATE, bidderId, id, maxPrice, increment);
        }

        try {
            autobidService.updateAutobid(bidderId, id, maxPrice, increment);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String deleteAutoBid(int bidderId, int id) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTOBID_DELETE, bidderId, id);
        }

        try {
            autobidService.deleteAutobid(bidderId, id);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    public List<AutoBid> getAutobidByBidder(int bidderId) {
        if (remoteApiClient != null) {
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.AUTOBID_GET_BY_BIDDER, bidderId);
                if (!response.isSuccess()) {
                    return List.of();
                }
                Object payload = response.getPayload();
                return payload instanceof List<?> items ? (List<AutoBid>) items : List.of();
            } catch (RuntimeException ex) {
                return List.of();
            }
        }

        return autobidService.getAutobidByBidder(bidderId);
    }

    public AutoBid getAutobidById(int id) {
        if (remoteApiClient != null) {
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.AUTOBID_GET_BY_ID, id);
                return response.isSuccess() ? response.payloadAs(AutoBid.class) : null;
            } catch (RuntimeException ex) {
                return null;
            }
        }

        return autobidService.getAutobid(id);
    }

    private String requestString(RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = remoteApiClient.send(action, arguments);
            if (!response.isSuccess()) {
                return response.getMessage();
            }
            String payload = response.payloadAsString();
            return payload == null || payload.isBlank() ? "SUCCESS" : payload;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }
}
