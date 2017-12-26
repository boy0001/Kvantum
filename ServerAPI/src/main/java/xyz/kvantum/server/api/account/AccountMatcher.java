/*
 *    _  __                     _
 *    | |/ /__   __ __ _  _ __  | |_  _   _  _ __ ___
 *    | ' / \ \ / // _` || '_ \ | __|| | | || '_ ` _ \
 *    | . \  \ V /| (_| || | | || |_ | |_| || | | | | |
 *    |_|\_\  \_/  \__,_||_| |_| \__| \__,_||_| |_| |_|
 *
 *    Copyright (C) 2017 IntellectualSites
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.kvantum.server.api.account;

import lombok.NonNull;
import xyz.kvantum.server.api.repository.Matcher;

/**
 * Matcher that matches account queries (often generated using
 * {@link xyz.kvantum.server.api.orm.KvantumObjectFactory}
 * <p>
 * Instances are built using {@link AccountMatcherFactory}
 *
 * @param <A> Query Type
 * @param <B> Value type
 */
public final class AccountMatcher<A extends IAccount, B extends IAccount> extends Matcher<A, B>
{

    AccountMatcher(@NonNull final A queryObject)
    {
        super( queryObject );
    }

    @Override
    protected boolean matches(@NonNull final A query,
                              @NonNull final B value)
    {
        return query.getUsername().equalsIgnoreCase( value.getUsername() )
                || query.getId() == value.getId();
    }
}
