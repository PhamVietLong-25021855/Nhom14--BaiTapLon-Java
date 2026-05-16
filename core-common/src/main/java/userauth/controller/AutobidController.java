package userauth.controller;

import userauth.api.AutobidApi;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;

import java.util.List;

/**
 * Thin controller layer for AutoBid operations.
 *
 * <p>All methods return {@code "SUCCESS"} on success or an error message string.
 * This matches the socket protocol where the response is either a string
 * or a wrapped server exception.</p>
 *
 * <p>Exceptions are caught and converted to error strings — no RuntimeException
 * is thrown from this class.</p>
 */
public class AutobidController {

    private final AutobidApi autobidService;

    public AutobidController(AutobidApi autobidService) {
        this.autobidService = autobidService;
    }

    /**
     * Create or update an auto-bid rule for (bidder, auction).
     *
     * Uses the upsert pattern — if a rule already exists it will be updated.
     *
     * @return "SUCCESS" or an error message
     */
    public String createAutobid(int bidderId, int auctionId, double maxPrice, double increment) {
        try {
            autobidService.createAutobid(bidderId, auctionId, maxPrice, increment);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "An unexpected error occurred while saving the auto-bid rule: " + e.getMessage();
        }
    }

    /**
     * Update an existing auto-bid rule by its id.
     *
     * @return "SUCCESS" or an error message
     */
    public String updateAutobid(int bidderId, int id, double maxPrice, double increment) {
        try {
            autobidService.updateAutobid(bidderId, id, maxPrice, increment);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "An unexpected error occurred while updating the auto-bid rule: " + e.getMessage();
        }
    }

    /**
     * Delete an auto-bid rule permanently.
     *
     * @return "SUCCESS" or an error message
     */
    public String deleteAutoBid(int bidderId, int id) {
        try {
            autobidService.deleteAutobid(bidderId, id);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "An unexpected error occurred while deleting the auto-bid rule: " + e.getMessage();
        }
    }

    /**
     * Get all auto-bid rules for a specific bidder.
     *
     * @return list of AutoBid rules (never null)
     */
    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autobidService.getAutobidByBidder(bidderId);
    }

    /**
     * Get a single auto-bid rule by its id.
     *
     * @return the AutoBid, or null if not found
     */
    public AutoBid getAutobidById(int id) {
        return autobidService.getAutobid(id);
    }
}
