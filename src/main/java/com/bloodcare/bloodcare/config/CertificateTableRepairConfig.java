package com.bloodcare.bloodcare.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CertificateTableRepairConfig {

    private static final Logger logger = LoggerFactory.getLogger(CertificateTableRepairConfig.class);
    private static final String ENTITY_MANAGER_FACTORY = "entityManagerFactory";
    private static final String REPAIR_BEAN = "certificateTableRepair";
    private static final String CERTIFICATE_TABLE = "certificate";

    @Bean(name = REPAIR_BEAN)
    InitializingBean certificateTableRepair(DataSource dataSource) {
        return () -> repairCertificateTable(dataSource);
    }

    @Bean
    static BeanFactoryPostProcessor certificateRepairDependencyPostProcessor() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition(ENTITY_MANAGER_FACTORY)) {
                return;
            }

            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition(ENTITY_MANAGER_FACTORY);
            Set<String> dependsOn = new LinkedHashSet<>();

            if (entityManagerFactory.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(entityManagerFactory.getDependsOn()));
            }

            dependsOn.add(REPAIR_BEAN);
            entityManagerFactory.setDependsOn(dependsOn.toArray(String[]::new));
        };
    }

    private static void repairCertificateTable(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String catalog = connection.getCatalog();

            if (!tableExists(connection, catalog, CERTIFICATE_TABLE)) {
                return;
            }

            if (tableIsReadable(connection)) {
                return;
            }

            logger.warn("Dropping broken '{}' table so Hibernate can recreate it cleanly.", CERTIFICATE_TABLE);

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS `" + CERTIFICATE_TABLE + "`");
            }
        }
    }

    private static boolean tableExists(Connection connection, String catalog, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(catalog, null, tableName, new String[] { "TABLE" })) {
            return tables.next();
        }
    }

    private static boolean tableIsReadable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet ignored = statement.executeQuery("SELECT 1 FROM `" + CERTIFICATE_TABLE + "` LIMIT 1")) {
            return true;
        } catch (SQLException exception) {
            if (isBrokenTable(exception)) {
                return false;
            }

            throw exception;
        }
    }

    private static boolean isBrokenTable(SQLException exception) {
        return exception.getErrorCode() == 1932 || messageContains(exception, "doesn't exist in engine");
    }

    private static boolean messageContains(Throwable throwable, String text) {
        Throwable current = throwable;

        while (current != null) {
            String message = current.getMessage();

            if (message != null && message.toLowerCase().contains(text)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
