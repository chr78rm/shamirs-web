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
public enum SliceProcessingState {
    NEW, CREATED, FETCHED, POSTED, EXPIRED;
    
    static public boolean isValid(String state) {
        boolean valid = true;
        try {
            valueOf(state);
        } catch (Exception e) {
            valid = false;
        }
        return valid;
    }
}
