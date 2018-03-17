package cu.pdi.bookstore.fx.gui.main;

import cu.pdi.bookstore.fx.components.ui.FXMLLocator;
import cu.pdi.bookstore.fx.components.ui.MenuAssembler;
import cu.pdi.bookstore.fx.components.ui.ResourceLocator;
import cu.pdi.bookstore.fx.enums.Roles;
import cu.pdi.bookstore.fx.enums.SimpleUIEvent;
import cu.pdi.bookstore.security.context.JaasSecurityContext;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.PopOver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class WorkspaceController implements Initializable {
    @FXML
    private Label personName;
    @FXML
    private Label rolName;
    @FXML
    private BorderPane root;
    @FXML
    private Button btnChangePassword;
    @FXML
    private MenuButton btnGeneralMenu;

    private final JaasSecurityContext jaasSecurityContext;
    private final ResourceLocator resourceLocator;
    private final FXMLLocator fxmlLocator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MenuAssembler menuAssembler;

    @Autowired
    public WorkspaceController(JaasSecurityContext jaasSecurityContext, ResourceLocator resourceLocator,
                               FXMLLocator fxmlLocator, ApplicationEventPublisher applicationEventPublisher,
                               MenuAssembler menuAssembler) {
        this.jaasSecurityContext = jaasSecurityContext;
        this.resourceLocator = resourceLocator;
        this.fxmlLocator = fxmlLocator;
        this.applicationEventPublisher = applicationEventPublisher;
        this.menuAssembler = menuAssembler;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jaasSecurityContext.authenticatedUsername().ifPresent(personName::setText);
        jaasSecurityContext.authenticatedUserRoleName().ifPresent(rolName::setText);

        initializeGeneralMenu();
        loadAuthorizedMenuActions();
        initializeChangePasswordPopOver();


    }

    private void initializeGeneralMenu() {
        btnGeneralMenu.setText("");
        btnGeneralMenu.setPopupSide(Side.RIGHT);
        resourceLocator.urlForImage("menu_icon.png")
                .ifPresent(imageUrl -> btnGeneralMenu.setGraphic(new ImageView(new Image(imageUrl))));

    }


    private void loadAuthorizedMenuActions() {
        Roles role = jaasSecurityContext.authenticatedUserRoleName()
                .map(Roles::valueOf)
                .orElseThrow(() -> new RuntimeException("No existent role"));
        menuAssembler.assembleForRole(role);
    }

    private void initializeChangePasswordPopOver() {
        PopOver popOver = new PopOver(
                fxmlLocator.getFXML("auth/change_password.fxml")
        );
        popOver.setAutoHide(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
        popOver.setArrowIndent(5d);
        popOver.setArrowSize(5d);
        popOver.setCloseButtonEnabled(false);
        popOver.setTitle(" Cambiar contraseña \n");
        popOver.setHeaderAlwaysVisible(true);
        popOver.setOnAutoHide(autoHideEvent -> {
            applicationEventPublisher.publishEvent(SimpleUIEvent.RESET_FIELDS);
        });
        popOver.setHideOnEscape(true);

        btnChangePassword.setOnAction(e -> popOver.show(btnChangePassword));
    }
}
