package springfox.documentation.swagger.util;

import com.google.common.collect.Multimap;
import io.swagger.models.*;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.ApiListing;
import springfox.documentation.service.Documentation;
import springfox.documentation.swagger.web.SwaggerResource;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

/**
 * @author chenyicheng
 * @version 1.0
 * @since 2019-08-18
 */
public abstract class SwaggerUtils {

    public final static String METHOD_ROUTER = "path";
    public final static String METHOD_TAGS = "tags";

    public static Swagger filter(HttpServletRequest request, Swagger swagger, Documentation documentation) {
        String path = request.getParameter(METHOD_ROUTER);
        String tags = request.getParameter(METHOD_TAGS);
        if (StringUtils.hasLength(path)) {
            Path pathObj = swagger.getPath(path);
            Map<String, Path> pathMap = new LinkedHashMap<String, Path>();
            if (pathObj != null) {
                pathMap.put(path, pathObj);
            } else {
                //match path
                Map<String, Path> pathAll = swagger.getPaths();
                for (Map.Entry<String, Path> entry : pathAll.entrySet()) {
                    if (entry.getKey().startsWith(path)) {
                        pathMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            swagger.setPaths(pathMap);

            //paths more  match tag
            if (pathMap.size() > 1) {
                filterPathByTags(request, swagger, tags);
            }
            //filter Definitions
            filterDefinitions(request, swagger, documentation);
            //filter tags
            filterTags(request, swagger);
        } else if (StringUtils.hasLength(tags)) {
            filterPathByTags(request, swagger, tags);
        }

        return swagger;
    }

    public static void filterPathByTags(HttpServletRequest request, Swagger swagger, String tags) {
        if (StringUtils.hasLength(tags)) {
            Map<String, Path> pathMap = swagger.getPaths();
            for (Iterator<Map.Entry<String, Path>> it = pathMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Path> entry = it.next();
                Path pathValue = entry.getValue();
                String[] tagNames = StringUtils.commaDelimitedListToStringArray(tags);
                filterOperationByTags(pathValue, tagNames);

                if (ObjectUtils.isEmpty(pathValue.getOperations())) {
                    it.remove();
                }
            }
        }
    }

    public static void filterDefinitions(HttpServletRequest request, Swagger swagger, Documentation documentation) {
        Map<String, Path> pathMap = swagger.getPaths();
        if (pathMap.isEmpty()) {
            return;
        }
        Set<String> modelNames = new HashSet<String>();
        Multimap<String, ApiListing> apiListings = documentation.getApiListings();
        for (ApiListing apiListing : apiListings.values()) {
            List<ApiDescription> apis = apiListing.getApis();
            for (ApiDescription api : apis) {
                if (pathMap.containsKey(api.getPath())) {
                    modelNames.addAll(apiListing.getModels().keySet());
                    break;
                }
            }
        }
        Map<String, Model> definitionsMap = swagger.getDefinitions();
        Map<String, Model> definitionsUse = new TreeMap<String, Model>();
        for (String key : modelNames) {
            Model model = definitionsMap.get(key);
            if (model != null) {
                definitionsUse.put(key, model);

            }

        }
        swagger.setDefinitions(definitionsUse);

    }

    public static Swagger filterTags(HttpServletRequest request, Swagger swagger) {
        if (ObjectUtils.isEmpty(swagger.getPaths())) {
            swagger.setTags(null);
            return null;
        }
        Set<Tag> tags = new HashSet<Tag>();
        for (Path path : swagger.getPaths().values()) {
            List<Operation> operations = path.getOperations();
            for (Operation operation : operations) {
                List<String> operationTags = operation.getTags();
                if (operationTags == null) {
                    continue;
                }
                for (String operationTag : operationTags) {
                    Tag tag = swagger.getTag(operationTag);
                    if (tag != null) {
                        tags.add(tag);
                    }
                }
            }
        }
        swagger.setTags(new ArrayList<Tag>(tags));
        return swagger;
    }


    public static List<SwaggerResource> queryPropagation(HttpServletRequest request, List<SwaggerResource> resources) {
        String query = request.getQueryString();
        if (StringUtils.hasLength(query)) {
            for (SwaggerResource resource : resources) {
                String location = resource.getLocation();
                URI uri = URI.create(location);
                String rawQuery = uri.getRawQuery();
                if (StringUtils.hasLength(rawQuery)) {
                    location = location + "&" + query;
                } else {
                    location = location + "?" + query;
                }
                resource.setLocation(location);
            }
        }
        return resources;
    }

    public static boolean containsTag(Operation operation, String tag) {
        if (operation == null || operation.getTags() == null) {
            return false;
        }
        for (String operationTag : operation.getTags()) {
            if (operationTag.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsTag(Operation operation, String[] tags) {
        for (String tag : tags) {
            if (containsTag(operation, tag)) {
                return true;
            }
        }
        return false;
    }

    public static void filterOperationByTags(Path path, String[] tags) {
        if (path == null) {
            return;
        }
        if (path.getGet() != null) {
            if (!containsTag(path.getGet(), tags)) {
                path.setGet(null);
            }
        }
        if (path.getPut() != null) {
            if (!containsTag(path.getPut(), tags)) {
                path.setPut(null);
            }
        }
        if (path.getHead() != null) {
            if (!containsTag(path.getHead(), tags)) {
                path.setHead(null);
            }
        }
        if (path.getPost() != null) {
            if (!containsTag(path.getPost(), tags)) {
                path.setPost(null);
            }
        }
        if (path.getDelete() != null) {
            if (!containsTag(path.getDelete(), tags)) {
                path.setDelete(null);
            }
        }
        if (path.getPatch() != null) {
            if (!containsTag(path.getPatch(), tags)) {
                path.setPatch(null);
            }
        }
        if (path.getOptions() != null) {
            if (!containsTag(path.getOptions(), tags)) {
                path.setOptions(null);
            }
        }
    }


}
