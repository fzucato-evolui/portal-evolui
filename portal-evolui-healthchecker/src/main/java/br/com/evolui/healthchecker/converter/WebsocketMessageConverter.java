package br.com.evolui.healthchecker.converter;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class WebsocketMessageConverter extends AbstractMessageConverter {
    private ObjectMapper objectMapper = this.initObjectMapper();
    @Nullable
    private Boolean prettyPrint;

    public WebsocketMessageConverter() {
        super(new MimeType("application", "json"));
    }

    public WebsocketMessageConverter(MimeType... supportedMimeTypes) {
        super(supportedMimeTypes);
    }

    private ObjectMapper initObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
        this.configurePrettyPrint();
    }

    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        this.configurePrettyPrint();
    }

    private void configurePrettyPrint() {
        if (this.prettyPrint != null) {
            this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
        }

    }

    protected boolean supports(Class<?> clazz) {
        return true;
    }

    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
        Charset charset = this.getContentTypeCharset(this.getMimeType(message.getHeaders()));
        Object payload = message.getPayload();
        return payload instanceof String ? payload : new String((byte[])((byte[])payload), charset);
    }

    @Nullable
    protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
        try {
            Class<?> view = this.getSerializationView(conversionHint);
            if (byte[].class == this.getSerializedPayloadClass()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                JsonEncoding encoding = this.getJsonEncoding(this.getMimeType(headers));
                JsonGenerator generator = this.objectMapper.getFactory().createGenerator(out, encoding);
                Throwable var8 = null;

                try {
                    if (view != null) {
                        this.objectMapper.writerWithView(view).writeValue(generator, payload);
                    } else {
                        this.objectMapper.writeValue(generator, payload);
                    }

                    payload = out.toByteArray();
                } catch (Throwable var18) {
                    var8 = var18;
                    throw var18;
                } finally {
                    if (generator != null) {
                        if (var8 != null) {
                            try {
                                generator.close();
                            } catch (Throwable var17) {
                                var8.addSuppressed(var17);
                            }
                        } else {
                            generator.close();
                        }
                    }

                }
            } else {
                Writer writer = new StringWriter(1024);
                if (view != null) {
                    this.objectMapper.writerWithView(view).writeValue(writer, payload);
                } else {
                    this.objectMapper.writeValue(writer, payload);
                }

                payload = writer.toString();
            }

            return payload;
        } catch (IOException var20) {
            throw new MessageConversionException("Could not write JSON: " + var20.getMessage(), var20);
        }
    }

    protected JsonEncoding getJsonEncoding(@Nullable MimeType contentType) {
        if (contentType != null && contentType.getCharset() != null) {
            Charset charset = contentType.getCharset();
            JsonEncoding[] var3 = JsonEncoding.values();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                JsonEncoding encoding = var3[var5];
                if (charset.name().equals(encoding.getJavaName())) {
                    return encoding;
                }
            }
        }

        return JsonEncoding.UTF8;
    }

    @Nullable
    protected Class<?> getSerializationView(@Nullable Object conversionHint) {
        if (conversionHint instanceof MethodParameter) {
            MethodParameter param = (MethodParameter)conversionHint;
            JsonView annotation = param.getParameterIndex() >= 0 ? (JsonView)param.getParameterAnnotation(JsonView.class) : (JsonView)param.getMethodAnnotation(JsonView.class);
            if (annotation != null) {
                return this.extractViewClass(annotation, conversionHint);
            }
        } else {
            if (conversionHint instanceof JsonView) {
                return this.extractViewClass((JsonView)conversionHint, conversionHint);
            }

            if (conversionHint instanceof Class) {
                return (Class)conversionHint;
            }
        }

        return null;
    }

    private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
        Class<?>[] classes = annotation.value();
        if (classes.length != 1) {
            throw new IllegalArgumentException("@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
        } else {
            return classes[0];
        }
    }

    private Charset getContentTypeCharset(@Nullable MimeType mimeType) {
        return mimeType != null && mimeType.getCharset() != null ? mimeType.getCharset() : Charset.forName("UTF-8");
    }

    static Type getResolvedType(Class<?> targetClass, @Nullable Object conversionHint) {
        if (conversionHint instanceof MethodParameter) {
            MethodParameter param = (MethodParameter)conversionHint;
            param = param.nestedIfOptional();
            if (Message.class.isAssignableFrom(param.getParameterType())) {
                param = param.nested();
            }

            Type genericParameterType = param.getNestedGenericParameterType();
            Class<?> contextClass = param.getContainingClass();
            return GenericTypeResolver.resolveType(genericParameterType, contextClass);
        } else {
            return targetClass;
        }
    }
}
