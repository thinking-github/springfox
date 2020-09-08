package springfox.documentation.swagger.util;

import com.google.common.collect.Multimap;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
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
 * SwaggerExpandedParameterBuilder
 *
 * @author chenyicheng
 * @version 1.0
 * @since 2019-08-18
 */
public abstract class SwaggerUtils {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerUtils.class);

    public final static String METHOD_ROUTER = "path";
    public final static String METHOD_TAGS = "tags";
    /**
     * 创建、修改时隐藏不显示接口参数
     */
    public static String UPDATE_IGNORE = "UHidden";
    /**
     * 查询时隐藏不显示接口参数
     */
    public static String QUERY_IGNORE = "QHidden";

    /**
     * 请求参数隐藏不显示接口参数
     */
    public static String REQUEST_IGNORE = "RequestHidden";

    /**
     * 创建和修改接口标记
     */
    public static String OPERATION_UPDATE = "update";

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

        // XXX: access value  contains RequestHidden Filter
        accessPropertyFilter(swagger, documentation);

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

    /**
     * access value  contains RequestHidden Filter
     *
     * @param swagger
     * @param documentation
     * @ApiOperation(value = "update",extensions=@Extension(properties = @ExtensionProperty(name = "update", value ="1")))
     */
    public static void accessPropertyFilter(Swagger swagger, Documentation documentation) {
        Map<String, Path> paths = swagger.getPaths();
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        for (Path path : paths.values()) {
            for (Operation operation : path.getOperations()) {
                Map<String, Object> vendorExtensions = operation.getVendorExtensions();
                Iterator<Parameter> iterator = operation.getParameters().iterator();
                while (iterator.hasNext()) {
                    Parameter parameter = iterator.next();
                    String access = parameter.getAccess();
                    if (vendorExtensions != null && vendorExtensions.containsKey(OPERATION_UPDATE)) {
                        Boolean readOnly = parameter.isReadOnly();
                        if (readOnly != null && readOnly) {
                            logger.info(parameter.toString() + " name={},readOnly={},paramAccess={}",
                                    parameter.getName(),parameter.isReadOnly(),access);
                            iterator.remove();
                            continue;
                        }
                    }

                    if (StringUtils.isEmpty(access)) {
                        continue;
                    }
                    //remove
                    if (access.contains(REQUEST_IGNORE)) {
                        logger.info(parameter.toString() + " name={},readOnly={},paramAccess={}",
                                parameter.getName(),parameter.isReadOnly(),access);
                        iterator.remove();
                        continue;
                    }
                }
                // create entityUpdate model
                if (vendorExtensions != null && vendorExtensions.containsKey(OPERATION_UPDATE)) {
                    for (Parameter parameter : operation.getParameters()) {
                        if (parameter instanceof BodyParameter) {
                            Model schema = ((BodyParameter) parameter).getSchema();
                            if (schema instanceof RefModel) {
                                RefModel refModel = (RefModel) schema;
                                String simpleName = refModel.getSimpleRef();
                                String nameUpdate = simpleName + "Update";
                                boolean modelToUpdate =  modelUpdate(swagger,simpleName,nameUpdate);
                                if (modelToUpdate) {
                                    //refModel clone
                                    RefModel refModelUpdate = new RefModel(nameUpdate);
                                    refModelUpdate.setDescription(refModel.getDescription());
                                    refModelUpdate.setProperties(refModel.getProperties());
                                    refModelUpdate.setExample(refModel.getExample());
                                    ((BodyParameter) parameter).setSchema(refModelUpdate);
                                }
                            } else if (schema instanceof ArrayModel) {
                                ArrayModel arrayModel = (ArrayModel) schema;
                                Property property = arrayModel.getItems();
                                if(property instanceof RefProperty){
                                    RefProperty refProperty = (RefProperty) property;
                                    String simpleName = refProperty.getSimpleRef();
                                    String nameUpdate = simpleName + "Update";
                                    boolean modelToUpdate =  modelUpdate(swagger,simpleName,nameUpdate);
                                    if(modelToUpdate){
                                        refProperty.set$ref(nameUpdate);
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * 当前实体含有大于等于三个只读属性时,重新生成少量参数的 '写实体'
     *
     * @param swagger
     * @param simpleName
     * @param nameUpdate
     * @return
     */
    public static boolean modelUpdate(Swagger swagger,String simpleName,String nameUpdate){
        Model modelEntity = swagger.getDefinitions().get(simpleName);
        Model modelUpdate = swagger.getDefinitions().get(nameUpdate);
        int readOnlyCount = 0;
        List<String> readOnlyList = null;
        for (Map.Entry<String, Property> propertyEntry : modelEntity.getProperties().entrySet()) {
            Property property = propertyEntry.getValue();
            if (property.getReadOnly() != null && property.getReadOnly()) {
                if (modelUpdate != null) {
                    readOnlyCount = 3;
                    break;
                }
                readOnlyCount++;
                if (readOnlyList == null) {
                    readOnlyList = new ArrayList<String>();
                }
                readOnlyList.add(propertyEntry.getKey());
            }
        }

        if (readOnlyCount >= 3) {
            ModelImpl cloneUpdate = (ModelImpl) swagger.getDefinitions().get(nameUpdate);
            if (cloneUpdate == null) {
                // model clone
                cloneUpdate = (ModelImpl) modelEntity.clone();
                cloneUpdate.setName(nameUpdate);
                cloneUpdate.setTitle(nameUpdate);
                for (String name : readOnlyList) {
                    cloneUpdate.getProperties().remove(name);
                }
                swagger.addDefinition(nameUpdate, cloneUpdate);
            }
        }
        return readOnlyCount >= 3;
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
