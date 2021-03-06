package io.hawt.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import io.hawt.util.MBeanSupport;
import io.hawt.jsonschema.internal.BeanValidationAnnotationModule;
import io.hawt.jsonschema.internal.IgnorePropertiesBackedByTransientFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author Stan Lewis
 */
public class SchemaLookup extends MBeanSupport implements SchemaLookupMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(SchemaLookup.class);

    private static SchemaLookup singleton;

    private ObjectMapper mapper;

    public SchemaLookup() {
    }

    public static SchemaLookup getSingleton() {
        if (singleton == null) {
            // lazy create one
            new SchemaLookup().init();
        }
        return singleton;
    }

    public void init() {
        LOG.debug("Creating hawtio SchemaLookup instance");
        try {
            if (mapper == null) {
                mapper = new ObjectMapper();

                mapper.setVisibilityChecker(new IgnorePropertiesBackedByTransientFields(mapper.getVisibilityChecker()));

                JaxbAnnotationModule module1 = new JaxbAnnotationModule();
                mapper.registerModule(module1);

                BeanValidationAnnotationModule module2 = new BeanValidationAnnotationModule();
                mapper.registerModule(module2);

            }
            // now lets expose the mbean...
            super.init();
            singleton = this;
        } catch (Exception e) {
            LOG.warn("Exception during initialization: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getDefaultObjectName() {
        return "io.hawt.jsonschema:type=SchemaLookup";
    }

    protected Class getClass(String name) {
        // TODO - well, this relies on DynamicImport-Package to work, but seems simpler than mucking about with org.osgi.framework.wiring
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            LOG.warn("Failed to find class for {}", name);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSchemaForClass(String name) {
        Class clazz = getClass(name);
        return getSchemaForClass(clazz);
    }

    public String getSchemaForClass(Class clazz) {
        LOG.info("Looking up schema for " + clazz.getCanonicalName());
        String name = clazz.getName();
        try {
            ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
            return writer.writeValueAsString(mapper.generateJsonSchema(clazz));
        } catch (Exception e) {
            LOG.warn("Failed to generate JSON schema for class {}", name, e);
            throw new RuntimeException(e);
        }
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }
}
