/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 *
 * @author Developer
 */
@Entity
@DiscriminatorValue("xml")
public class XMLDocument extends Document {
    
    private static final long serialVersionUID = 1L;

    public XMLDocument() {
    }

    public XMLDocument(String id) {
        super(id);
    }
    
}
