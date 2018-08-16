/*
 *    _  __                     _
 *    | |/ /__   __ __ _  _ __  | |_  _   _  _ __ ___
 *    | ' / \ \ / // _` || '_ \ | __|| | | || '_ ` _ \
 *    | . \  \ V /| (_| || | | || |_ | |_| || | | | | |
 *    |_|\_\  \_/  \__,_||_| |_| \__| \__,_||_| |_| |_|
 *
 *    Copyright (C) 2018 IntellectualSites
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
package xyz.kvantum.server.api.util;

import java.util.Map;
import java.util.Optional;
import xyz.kvantum.server.api.request.AbstractRequest;

/**
 * A {@link VariableProvider} implementation for meta variables
 */
public final class MetaProvider implements ProviderFactory<MetaProvider>, VariableProvider
{

	private AbstractRequest r;

	public MetaProvider()
	{
	}

	private MetaProvider(final AbstractRequest r)
	{
		this.r = r;
	}

	@Override public Optional<MetaProvider> get(final AbstractRequest r)
	{
		return Optional.of( new MetaProvider( r ) );
	}

	@Override public String providerName()
	{
		return "meta";
	}

	@Override public boolean contains(final String variable)
	{
		return r.getMeta( variable ) != null;
	}

	@Override public Object get(final String variable)
	{
		return r.getMeta( variable );
	}

	@Override public Map<String, Object> getAll()
	{
		return r.getAllMeta();
	}
}
