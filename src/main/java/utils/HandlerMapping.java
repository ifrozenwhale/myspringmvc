package utils;

import annotation.MyRequestMapping;
import annotation.ResponseBody;
import annotation.ResponseType;
import annotation.ResponseView;
import ioc.IOC;

import javax.servlet.ServletConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class HandlerMapping {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    //    private Map<String, Object> ioc = new HashMap<>();
    private IOC ioc = new IOC();

    private Map<String, MVCMapping> handlerMapping = new HashMap<>();

    public HandlerMapping(ServletConfig config) {
        loadConfig(config);
        loadClassNames(properties.getProperty("testModule"));
        loadClass();
        loadMapping();
    }

    public Map<String, MVCMapping> getAllMappings(){
        return handlerMapping;
    }

    private void loadMapping() {
        for(Map.Entry<String, Object> entry : ioc.getAllObject()){
            Class<?> cla = entry.getValue().getClass();

            String baseUrl = "";
            if(cla.isAnnotationPresent(MyRequestMapping.class)){
                baseUrl = cla.getAnnotation(MyRequestMapping.class).value();
            }

            Method[] methods = cla.getMethods();
            for(Method method : methods){
//                Annotation[] as = method.getAnnotations();
//                for (Annotation annotation : as) {
//                    System.out.println(annotation.toString());
//                }
                if(!method.isAnnotationPresent(MyRequestMapping.class)){
                    continue;
                }
                String url = method.getAnnotation(MyRequestMapping.class).value();
                if(method.isAnnotationPresent(ResponseBody.class)){
                    try {
                        handlerMapping.put(baseUrl+url, new MVCMapping(method, ResponseType.Text, cla.newInstance()));
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                else if (method.isAnnotationPresent(ResponseView.class)){
                    try {
                        handlerMapping.put(baseUrl+url, new MVCMapping(method, ResponseType.View, cla.newInstance()));
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

                try {
//                    controllerMap.put(baseUrl+url, cla.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void loadClass() {
        for(String className : classNames){
            try {
                ioc.addObject(className);
//                Class<?> cla = Class.forName(className);
//                if(cla.isAnnotationPresent(MyController.class)){
//                    ioc.put(cla.getName(), cla.newInstance());
//                }
//                else{
//                    continue;
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadClassNames(String packageName) {
        URL url = this.getClass().getClassLoader().getResource(packageName);
        File testModule = new File(url.getFile());
        for(File file : testModule.listFiles()){
            if(file.isDirectory()){
                loadClassNames(packageName+"."+file.getName());
            }
            else {
                String className = packageName +"."+ file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void loadConfig(ServletConfig config){
        String configLocation = config.getInitParameter("Configuration");
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream(configLocation);
        try {
            properties.load(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            resource.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
