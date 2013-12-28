/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package github.madmarty.madsonic.service;


/**
 * Thrown if the REST API version implemented by the serverType is too old.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class ServerTypeToOldException extends Exception {

    private final String text;
    private final String serverType;
    private final String requiredType;

    public ServerTypeToOldException(String text, String serverType, String requiredType) {
        this.text = text;
        this.serverType = serverType;
        this.requiredType = requiredType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (text != null) {
            builder.append(text).append(" ");
        }
        builder.append("Server Type too old. ");
        builder.append("Requires ").append(requiredType).append(" but is ").append(serverType).append(".");
        return builder.toString();
    }
}
