//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//     IntellectualServer is a web server, written entirely in the Java language.                            /
//     Copyright (C) 2015 IntellectualSites                                                                  /
//                                                                                                           /
//     This program is free software; you can redistribute it and/or modify                                  /
//     it under the terms of the GNU General Public License as published by                                  /
//     the Free Software Foundation; either version 2 of the License, or                                     /
//     (at your option) any later version.                                                                   /
//                                                                                                           /
//     This program is distributed in the hope that it will be useful,                                       /
//     but WITHOUT ANY WARRANTY; without even the implied warranty of                                        /
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                         /
//     GNU General Public License for more details.                                                          /
//                                                                                                           /
//     You should have received a copy of the GNU General Public License along                               /
//     with this program; if not, write to the Free Software Foundation, Inc.,                               /
//     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.                                           /
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.intellectualsites.web.object.syntax;

import com.intellectualsites.web.object.Request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class IfStatement extends Syntax {

    public IfStatement() {
        super(Pattern.compile("\\{(#if)( !| )([A-Za-z0-9]*).([A-Za-z0-9_\\-@]*)\\}([\\S\\s]*?)\\{(/if)\\}"));
    }

    @Override
    public String process(String in, Matcher matcher, Request r, Map<String, ProviderFactory> factories) {
        while (matcher.find()) {
            String neg = matcher.group(2), namespace = matcher.group(3), variable = matcher.group(4);
            if (factories.containsKey(namespace.toLowerCase())) {
                VariableProvider p = factories.get(namespace.toLowerCase()).get(r);
                if (p != null) {
                    if (p.contains(variable)) {
                        Object o = p.get(variable);
                        boolean b;
                        if (o instanceof Boolean) {
                            b = (Boolean) o;
                        } else if (o instanceof String) {
                            b = o.toString().toLowerCase().equals("true");
                        } else
                            b = o instanceof Number && ((Number) o).intValue() == 1;
                        if (neg.contains("!")) {
                            b = !b;
                        }

                        if (b) {
                            in = in.replace(matcher.group(), matcher.group(5));
                        } else {
                            in = in.replace(matcher.group(), "");
                        }
                    }
                }
            }
        }
        return in;
    }
}
