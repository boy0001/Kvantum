package com.plotsquared.iserver.views;

import com.plotsquared.iserver.files.Path;
import com.plotsquared.iserver.object.Header;
import com.plotsquared.iserver.object.Request;
import com.plotsquared.iserver.object.Response;
import com.plotsquared.iserver.util.FileExtension;
import com.plotsquared.iserver.util.Logger;

import java.util.*;

public abstract class StaticFileView extends View
{

    private final Collection<FileExtension> extensionList;

    public StaticFileView(String filter, Map<String, Object> options, String name, Collection<FileExtension> extensions)
    {
        super( filter, name, options );
        this.extensionList = extensions;
    }

    @Override
    final public boolean passes(final Request request)
    {
        Logger.info( "StaticFileView: Incoming request!" );

        final Map<String, String> variables = request.getVariables();
        FileExtension fileExtension;
        if ( !variables.containsKey( "extension" ) || variables.get( "extension" ).isEmpty() )
        {
            Logger.debug( "No given extension, trying to find it instead!" );
            if ( containsOption( "defaultExtension" ) )
            {
                variables.put( "extension", getOption( "defaultExtension" ) );
            } else
            {
                variables.put( "extension", extensionList.iterator().next().getExtension() );
            }
        }
        check:
        {
            for ( final FileExtension extension : extensionList )
            {
                if ( extension.matches( variables.get( "extension" ) ) )
                {
                    fileExtension = extension;
                    break check;
                }
            }
            Logger.error( "Unknown file extension: " + variables.get( "extension" ) );
            return false; // None matched
        }
        request.addMeta( "extension", fileExtension );
        final Path file = getFile( request );
        request.addMeta( "file", file );
        final boolean exists = file.exists();
        if ( exists )
        {
            request.addMeta( "file_length", file.length() );
        }
        return exists;
    }

    @Override
    public Response generate(Request r)
    {
        final Path path = (Path) r.getMeta( "file" );
        final FileExtension extension = (FileExtension) r.getMeta( "extension" );
        final Response response = new Response( this );
        response.getHeader().set( Header.HEADER_CONTENT_TYPE, extension.getContentType() );
        if ( extension.getReadType() == FileExtension.ReadType.BYTES )
        {
            response.setBytes( path.readBytes() );
        } else
        {
            response.setContent( path.readFile() );
        }
        return response;
    }
}
