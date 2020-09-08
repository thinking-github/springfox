package springfox.documentation.spring.web.utils;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import io.swagger.annotations.ApiImplicitParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.Operation;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.service.contexts.OperationContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author thinking
 * @version 1.0
 * @since 2019-09-12
 */
public class SwaggerAnnotationUtils {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerAnnotationUtils.class);
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


    private final static TypeResolver TYPE_RESOLVER = new TypeResolver();


    /**
     * ApiImplicitParam support dataTypeClass query form
     *
     * @param context
     * @return
     */
    public static List<ResolvedMethodParameter> readApiImplicitParamWithDataTypeClass(OperationContext context) {
        List<ResolvedMethodParameter> methodParameters = context.getParameters();
        Optional<ApiImplicitParam> annotation = context.findAnnotation(ApiImplicitParam.class);
        if (annotation.isPresent() && (annotation.get().dataTypeClass() != null || annotation.get().dataTypeClass() != Void.class)) {
            Class<?> dataTypeClass = annotation.get().dataTypeClass();
            if (methodParameters == null) {
                methodParameters = new ArrayList<ResolvedMethodParameter>();
            }
            ResolvedType resolvedType = TYPE_RESOLVER.resolve(dataTypeClass);
            ResolvedMethodParameter virtualParameter = new ResolvedMethodParameter(
                    methodParameters.size(),
                    "model",
                    new ArrayList<Annotation>(0),
                    resolvedType);
            methodParameters.add(virtualParameter);

        }
        return methodParameters;
    }

    /**
     *  access value  contains RequestHidden Filter
     * @param apiDescriptions
     */
    public static void accessPropertyFilter(Collection<ApiDescription> apiDescriptions) {
        if (CollectionUtils.isEmpty(apiDescriptions)) {
            return;
        }
        for (ApiDescription apiDescription : apiDescriptions) {
            for (Operation operation : apiDescription.getOperations()) {

                for (Parameter parameter : operation.getParameters()) {
                    String access = parameter.getParamAccess();
                    if (StringUtils.isEmpty(access)) {
                        continue;
                    }
                    //remove
                    if(access.contains(REQUEST_IGNORE)){
                        // TODO: 2019-10-31 guava  ImmutableList  thinking
                        logger.info(parameter.toString() +" paramAccess="+access);

                    }
                }
            }
        }
    }


}
