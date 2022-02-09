package de.christofreichardt.shamirsweb.test;

import java.util.List;

/**
 *
 * @author Developer
 */
public class RemoteMariaDB extends MariaDB {

    public RemoteMariaDB() {
        super(List.of("mysql", "--defaults-extra-file=shamir-db.user.ini", "--verbose", "--host=shamirs-db"));
    }
    
}
