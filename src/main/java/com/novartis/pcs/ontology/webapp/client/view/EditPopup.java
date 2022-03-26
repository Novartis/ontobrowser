package com.novartis.pcs.ontology.webapp.client.view;

import com.google.gwt.user.client.ui.DialogBox;

public abstract class EditPopup {
    private final DialogBox dialogBox = new DialogBox(false, true);

    public void show() {
        dialogBox.center();
    }
}
