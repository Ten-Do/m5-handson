import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Fake Spring Web annotations so OrderApi compiles without Spring on the
// classpath. Names and attributes mirror org.springframework.web.bind.annotation.

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
@interface RestController {}

@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
@interface RequestMapping { String value() default ""; }

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
@interface GetMapping { String value() default ""; }

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
@interface PostMapping { String value() default ""; }

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
@interface DeleteMapping { String value() default ""; }

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
@interface PathVariable { String value() default ""; }

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
@interface RequestBody {}
