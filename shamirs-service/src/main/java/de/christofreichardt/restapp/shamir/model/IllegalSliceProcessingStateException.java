/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.christofreichardt.restapp.shamir.model;

/**
 *
 * @author Developer
 */
public class IllegalSliceProcessingStateException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public IllegalSliceProcessingStateException(String message) {
        super(message);
    }
    
}
