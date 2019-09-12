package springfox.documentation.spring.web.utils;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import io.swagger.annotations.ApiImplicitParam;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.service.contexts.OperationContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author thinking
 * @version 1.0
 * @since 2019-09-12
 */
public class SwaggerAnnotationUtils {

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
}
