package xyz.kvantum.server.api.request;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import xyz.kvantum.server.api.config.CoreConfig;
import xyz.kvantum.server.api.core.ServerImplementation;
import xyz.kvantum.server.api.exceptions.RequestException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({ "unused", "WeakerAccess" })
@UtilityClass
public class RequestCompiler
{

    private static final Pattern PATTERN_QUERY = Pattern.compile(
            "(?<method>[A-Za-z]+) (?<resource>[/\\-A-Za-z0-9.?=&:@!%]*) " +
                    "(?<protocol>(?<prottype>[A-Za-z]+)/(?<protver>[A-Za-z0-9.]+))?"
    );
    private static final Pattern PATTERN_HEADER = Pattern.compile( "(?<key>[A-Za-z-_0-9]+)\\s*:\\s*(?<value>.*$)" );

    public static Optional<HeaderPair> compileHeader(@NonNull final String line)
    {
        final Matcher matcher = PATTERN_HEADER.matcher( line );
        if ( !matcher.matches() )
        {
            return Optional.empty();
        }
        return Optional.of( new HeaderPair( matcher.group( "key" ).toLowerCase(),
                matcher.group( "value" ) ) );
    }

    public static void compileQuery(@NonNull final AbstractRequest request, @NonNull final String line)
            throws IllegalArgumentException, RequestException
    {
        final Matcher matcher = PATTERN_QUERY.matcher( line );
        if ( !matcher.matches() )
        {
            throw new IllegalArgumentException( "Not a query line" );
        }
        if ( CoreConfig.verbose )
        {
            ServerImplementation.getImplementation().log( "Query: " + matcher.group() );
        }
        final Optional<HttpMethod> methodOptional = HttpMethod.getByName( matcher.group( "method" ) );
        if ( !methodOptional.isPresent() )
        {
            throw new RequestException( "Unknown request method: " + matcher.group( "method" ),
                    request );
        }
        request.setQuery( new AbstractRequest.Query( methodOptional.get(), matcher.group( "resource" ) ) );
    }

    @Getter
    @RequiredArgsConstructor
    public static final class HeaderPair
    {

        @NonNull
        private final String key;
        @NonNull
        private final String value;
    }

}
