package userauth.gui.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;

public class ServerOfflineViewController {
    @FXML
    private VBox offlineCard;

    @FXML
    private Label lblEndpoint;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblSteps;

    @FXML
    private Button btnRetry;

    private Runnable retryHandler = () -> {};
    private Runnable closeHandler = () -> {};

    @FXML
    private void initialize() {
        Platform.runLater(() -> UiEffects.playEntrance(offlineCard, 120, 24, 0));
    }

    @FXML
    private void handleRetry() {
        retryHandler.run();
    }

    @FXML
    private void handleClose() {
        closeHandler.run();
    }

    public void setRetryHandler(Runnable retryHandler) {
        this.retryHandler = Objects.requireNonNullElse(retryHandler, () -> {});
    }

    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = Objects.requireNonNullElse(closeHandler, () -> {});
    }

    public void setServerEndpoint(String endpoint) {
        if (lblEndpoint != null) {
            lblEndpoint.setText(UiText.text(endpoint == null || endpoint.isBlank()
                    ? "Server endpoint is not configured."
                    : "Target server: " + endpoint));
        }
    }

    public void showOfflineState(String detailMessage) {
        if (lblStatus != null) {
            lblStatus.setText(UiText.text(detailMessage == null || detailMessage.isBlank()
                    ? "The auction server is offline or unreachable."
                    : detailMessage));
        }
        if (lblSteps != null) {
            lblSteps.setText(UiText.text(
                    "Start run-server.ps1, verify the host and port, then press Try again."
            ));
        }
        setBusy(false);
    }

    public void showCheckingState() {
        if (lblStatus != null) {
            lblStatus.setText(UiText.text("Checking connection to the auction server..."));
        }
        if (lblSteps != null) {
            lblSteps.setText(UiText.text("Please wait while the client retries the connection."));
        }
        setBusy(true);
    }

    private void setBusy(boolean busy) {
        if (offlineCard != null) {
            offlineCard.setDisable(busy);
        }
        if (btnRetry != null) {
            btnRetry.setText(UiText.text(busy ? "CHECKING..." : "TRY AGAIN"));
        }
    }
}
