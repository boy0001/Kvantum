/**
 * IntellectualServer is a web server, written entirely in the Java language.
 * Copyright (C) 2015 IntellectualSites
 *
 * This program is free software; you can redistribute it andor modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.github.intellectualsites.iserver;

import com.github.intellectualsites.iserver.api.config.CoreConfig;
import com.github.intellectualsites.iserver.api.config.YamlConfiguration;
import com.github.intellectualsites.iserver.api.core.ServerImplementation;
import com.github.intellectualsites.iserver.api.exceptions.IntellectualServerException;
import com.github.intellectualsites.iserver.api.util.TimeUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A MySQL Connection utility
 *
 * @author peelsh
 * @author Citymonstret
 */
final public class MySQLConnManager
{

    private String host;
    private int port;
    private String db;
    private String user;
    private String pass;

    private Connection conn;

    MySQLConnManager()
    {
        try
        {
            YamlConfiguration config = new YamlConfiguration( "mysql", new File( new File( ServerImplementation.getImplementation()
                    .getCoreFolder(),
                    "config" ), "mysql.yml" ) );
            config.loadFile();
            this.host = config.get( "mysql.host", "127.0.0.1" );
            this.pass = config.get( "mysql.pass", "password" );
            this.user = config.get( "mysql.user", "root" );
            this.port = config.get( "mysql.port", 3306 );
            this.db = config.get( "mysql.db", "database" );
            config.saveFile();
        } catch ( final Exception e )
        {
            throw new MySQLInitiationException( "Could not load mysql.yml", e );
        }
        log( "MySQL manager is created!" );
    }

    public Connection getConnection()
    {
        return conn;
    }

    void init()
    {
        String connUrl = "jdbc:mysql://" + host + ":" + port + "/" + db;
        try
        {
            conn = DriverManager.getConnection( connUrl, user, pass );
            log( "Connection established." );
        } catch ( SQLException ex )
        {
            ex.printStackTrace();
            // log("MySQL threw error: ", ex.getMessage() + ex.getErrorCode());
        }
    }

    private void log(final String message, final Object... args)
    {
        String msg = message;
        for ( final Object a : args )
        {
            msg = msg.replaceFirst( "%s", a.toString() );
        }
        System.out.printf( "[%s][%s] %s\n", CoreConfig.logPrefix + "-MySQL", TimeUtil.getTimeStamp(), msg );
    }

    private class MySQLInitiationException extends IntellectualServerException
    {

        MySQLInitiationException(final String issue, final Exception cause)
        {
            super( issue, cause );
        }

    }
}
