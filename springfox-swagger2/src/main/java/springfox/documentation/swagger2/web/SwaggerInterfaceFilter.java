package springfox.documentation.swagger2.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import springfox.documentation.swagger.web.UiConfiguration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * SwaggerInterfaceFilter path filter （Controller Method URL）
 *
 * @author chenyicheng
 * @version 1.0
 * @since 2019-08-17
 */
@Component
@WebFilter(filterName = "swaggerInterfaceFilter", urlPatterns = {"/**"})
public class SwaggerInterfaceFilter extends OncePerRequestFilter {

    public final static String API_DOCS = "api-docs";
    public final static String API_DOCS_1 = "api-docs=1";
    public final static String API_DOCS_TRUE = "api-docs=true";

    public final static boolean HAS_DEFAULT_MODELS_EXPAND_DEPTH;


    private static final Logger logger = LoggerFactory.getLogger(SwaggerInterfaceFilter.class);

    static {
        Field field = ReflectionUtils.findField(UiConfiguration.class, "defaultModelsExpandDepth");
        HAS_DEFAULT_MODELS_EXPAND_DEPTH = field == null ? false : true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String api_docs = request.getParameter(API_DOCS);
        if (Boolean.valueOf(api_docs) || "1".equals(api_docs)) {
            String baseURL = getBaseURL(request);
            String location = baseURL + "/swagger-ui.html?path=" + request.getRequestURI();
            String query = request.getQueryString();
            if (StringUtils.hasLength(query)) {
                if (query.contains(API_DOCS_1)) {
                    query = query.replaceAll(API_DOCS_1, "1=1");
                } else {
                    query = query.replaceAll(API_DOCS_TRUE, "1=1");
                }
                location = location + "&" + query;

                if (!query.contains("docExpansion")) {
                    location = location + "&docExpansion=list";
                }
                if(!query.contains("defaultModelsExpandDepth") && HAS_DEFAULT_MODELS_EXPAND_DEPTH){
                    location = location + "&defaultModelsExpandDepth=-1";
                }
            }
            response.sendRedirect(location);
        } else {
            filterChain.doFilter(request, response);
        }

    }

    @Override
    public void afterPropertiesSet() throws ServletException {
        logger.info("swaggerInterfaceFilter initialize...");
    }


    public String getBaseURL(HttpServletRequest request) {
        StringBuffer url = new StringBuffer();
        String scheme = request.getScheme();
        int port = request.getServerPort();
        if (port < 0) {
            port = 80;
        }

        url.append(scheme);
        url.append("://");
        url.append(request.getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }

        return url.toString();
    }
}
