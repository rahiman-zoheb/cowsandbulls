package com.zohebrahiman.cowsandbulls.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React app from the same origin as the API (#46), so the
 * browser needs no CORS and no proxy. The image copies the build into
 * classpath:/static; without it (backend-only development) nothing is served
 * here and every non-API path is a 404.
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

	private static final String STATIC = "classpath:/static/";

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Vite puts a content hash in every asset's file name, so a changed
		// file always has a new URL and old ones can be cached indefinitely.
		registry.addResourceHandler("/assets/**")
				.addResourceLocations(STATIC + "assets/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

		// Everything else, including index.html, is revalidated on every load,
		// so a deploy shows up straight away.
		registry.addResourceHandler("/**")
				.addResourceLocations(STATIC)
				.setCacheControl(CacheControl.noCache())
				.resourceChain(true)
				.addResolver(new ClientRouteResolver());
	}

	/**
	 * Client-side routes like /games/{id} exist only in the React router, so
	 * the server answers them with index.html. Two kinds of path are excluded,
	 * because returning HTML for them would hide a real error behind a 200:
	 * anything under api/ or actuator/, and anything with a file extension,
	 * such as a missing script.
	 */
	static final class ClientRouteResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String path, Resource location) throws IOException {
			// An empty path (the root) or one ending in "/" names a directory,
			// which also "exists"; serving it would bypass index.html.
			boolean namesAFile = !path.isEmpty() && !path.endsWith("/");
			Resource requested = location.createRelative(path);
			if (namesAFile && requested.exists() && requested.isReadable()) {
				return requested;
			}
			if (isClientRoute(path)) {
				Resource index = location.createRelative("index.html");
				return index.exists() && index.isReadable() ? index : null;
			}
			return null;
		}

		static boolean isClientRoute(String path) {
			if (path.startsWith("api/") || path.equals("api") || path.startsWith("actuator/") || path.equals("actuator")) {
				return false;
			}
			String lastSegment = path.substring(path.lastIndexOf('/') + 1);
			return !lastSegment.contains(".");
		}
	}
}
