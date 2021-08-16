/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.common;

/**
 *
 * @author Developer
 */
public enum SessionPhase {
    NEW, PROVISIONED, ACTIVE, CLOSED;
    
    static public boolean isValid(String phase) {
        boolean valid = true;
        try {
            valueOf(phase);
        } catch (Exception e) {
            valid = false;
        }
        return valid;
    }
}
