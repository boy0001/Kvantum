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
package xyz.kvantum.server.implementation;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.NonNull;
import xyz.kvantum.files.FileSystem;
import xyz.kvantum.server.api.config.ConfigurationFile;
import xyz.kvantum.server.api.config.CoreConfig;
import xyz.kvantum.server.api.config.YamlConfiguration;
import xyz.kvantum.server.api.core.ServerImplementation;
import xyz.kvantum.server.api.logging.Logger;
import xyz.kvantum.server.api.views.Decorator;
import xyz.kvantum.server.api.views.View;

/**
 * Used to load view configuration (often generated by {@link xyz.kvantum.server.api.views.ViewDetector} into {@link
 * View views}
 */
@SuppressWarnings("ALL") final class ViewLoader
{

	private final Supplier<FileSystem> fileSystemSupplier;
	private final Collection<Decorator> decorators;
	private Map<String, Map<String, Object>> views = new HashMap<>();
	private Map<String, Class<? extends View>> viewBindings;

	ViewLoader(@NonNull final ConfigurationFile viewConfiguration,
			@NonNull final Supplier<FileSystem> fileSystemSupplier, @NonNull final Collection<Decorator> decorators,
			@NonNull final Map<String, Class<? extends View>> viewBindings)
	{
		this.fileSystemSupplier = fileSystemSupplier;
		this.viewBindings = viewBindings;
		this.decorators = decorators;
		this.addViews( viewConfiguration );
		this.views.entrySet().forEach( this::loadView );
	}

	ViewLoader(@NonNull final Supplier<FileSystem> fileSystemSupplier, @NonNull final Collection<Decorator> decorators,
			@NonNull final Map<String, Map<String, Object>> views,
			@NonNull final Map<String, Class<? extends View>> viewBindings)
	{
		this.fileSystemSupplier = fileSystemSupplier;
		this.decorators = decorators;
		this.viewBindings = viewBindings;
		this.views = views;
		this.views.entrySet().forEach( this::loadView );
	}

	private void visitMembers(@Nullable final Map<String, Object> views)
	{
		if ( views == null || views.isEmpty() )
		{
			return;
		}
		for ( final Map.Entry<String, Object> viewEntry : views.entrySet() )
		{
			if ( viewEntry.getValue() == null )
			{
				continue;
			}
			if ( viewEntry.getValue() instanceof String )
			{
				final String object = viewEntry.getValue().toString();
				final ConfigurationFile includeFile;
				if ( object.endsWith( ".yml" ) )
				{
					try
					{
						includeFile = new YamlConfiguration( object, new File(
								new File( ServerImplementation.getImplementation().getCoreFolder(), "config" ),
								object ) );
						includeFile.loadFile();
						if ( !includeFile.contains( "views" ) )
						{
							continue;
						}
					} catch ( Exception e )
					{
						new RuntimeException( "Failed to include views file", e ).printStackTrace();
						continue;
					}
				} else
				{
					Logger.warn( "Trying to include view declaration " + "that is not of YAML type: {}", object );
					continue;
				}
				this.addViews( includeFile );
			} else
			{
				final Object rawObject = viewEntry.getValue();
				if ( rawObject instanceof Map )
				{
					this.views.put( viewEntry.getKey(), ( Map<String, Object> ) viewEntry.getValue() );
				}
			}
		}
	}

	private void loadView(@Nullable final Map.Entry<String, Map<String, Object>> viewEntry)
	{
		if ( viewEntry == null || viewEntry.getValue() == null )
		{
			return;
		}

		final Map<String, Object> viewBody = viewEntry.getValue();

		if ( !validateView( viewBody ) )
		{
			Logger.warn( "Invalid view declaration: {}", viewEntry.getKey() );
			return;
		}

		final String type = viewBody.get( "type" ).toString().toLowerCase( Locale.ENGLISH );
		final String filter = viewBody.get( "filter" ).toString();
		final Map<String, Object> options = ( Map<String, Object> ) viewBody.getOrDefault( "options", new HashMap<>() );

		//
		// Store internal reference to view entry name
		//
		options.put( "internalName", viewEntry.getKey() );

		if ( viewBindings.containsKey( type ) )
		{
			final Class<? extends View> vc = viewBindings.get( type.toLowerCase( Locale.ENGLISH ) );
			try
			{
				final View vv = vc.getDeclaredConstructor( String.class, Map.class ).newInstance( filter, options );
				vv.setFileSystemSupplier( this.fileSystemSupplier );
				this.decorators.forEach( vv::addResponseDecorator );
				if ( CoreConfig.debug )
				{
					Logger.debug( "Added view " + vv.getName() );
				}
				ServerImplementation.getImplementation().getRouter().add( vv );
			} catch ( final Exception e )
			{
				new RuntimeException( "Failed to add view '" + viewEntry.getKey() + "' to router", e )
						.printStackTrace();
			}
		} else
		{
			Logger.warn( "View declaration '{}' trying to declare unknown type: {}", viewEntry.getKey(), type );
		}
	}

	private boolean validateView(@NonNull final Map<String, Object> viewBody)
	{
		return viewBody.containsKey( "type" ) && viewBody.containsKey( "filter" );
	}

	private void addViews(@Nullable final ConfigurationFile file)
	{
		if ( file == null || file.getAll().isEmpty() )
		{
			return;
		}
		final Map<String, Object> rawEntries = file.get( "views" );
		if ( rawEntries == null || rawEntries.isEmpty() )
		{
			return;
		}
		this.visitMembers( rawEntries );
	}
}
