package inject4j.core.context;

import java.util.List;

public interface BeanFactoriesLoader {

    List<BeanFactory> getBeanFactories(Environment env, AppBeanContext context);

}
