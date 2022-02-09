package de.christofreichardt.shamirsweb.test;

import java.util.List;

/**
 *
 * @author Developer
 */
public class NativeMariaDB extends MariaDB {

    public NativeMariaDB() {
        super(List.of("mysql", "--defaults-extra-file=shamir-db.user.ini", "--verbose"));
    }
    
}
