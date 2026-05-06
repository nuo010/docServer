package com.example.docserver.config;

import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OfficeConfig {

    @Bean
    public ManagedOfficeManager managedOfficeManager() {
        return new ManagedOfficeManager();
    }

    @Bean
    public OfficeManager officeManager(ManagedOfficeManager managedOfficeManager) {
        return managedOfficeManager.getOfficeManager();
    }

    public static class ManagedOfficeManager implements InitializingBean, DisposableBean {

        private final OfficeManager officeManager;

        public ManagedOfficeManager() {
            this.officeManager = LocalOfficeManager.builder().install().build();
        }

        public OfficeManager getOfficeManager() {
            return officeManager;
        }

        @Override
        public void afterPropertiesSet() throws OfficeException {
            officeManager.start();
        }

        @Override
        public void destroy() throws OfficeException {
            officeManager.stop();
        }
    }
}
