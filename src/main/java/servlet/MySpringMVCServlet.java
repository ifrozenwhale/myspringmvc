package servlet;

import annotation.MyController;
import annotation.MyRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MySpringMVCServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMapping = new  HashMap<>();

    private Map<String, Object> controllerMap  =new HashMap<>();


    @Override
    public void init(ServletConfig config) {
        loadConfig(config);
        loadClassNames(properties.getProperty("testModule"));
        loadClass();
        loadMapping();
    }

    private void loadMapping() {
        for(Map.Entry<String, Object> entry : ioc.entrySet()){
            Class<?> cla = entry.getValue().getClass();

            String baseUrl = "";
            if(cla.isAnnotationPresent(MyRequestMapping.class)){
                baseUrl = cla.getAnnotation(MyRequestMapping.class).value();
            }

            Method[] methods = cla.getMethods();
            for(Method method : methods){
                if(!method.isAnnotationPresent(MyRequestMapping.class)){
                    continue;
                }
                String url = method.getAnnotation(MyRequestMapping.class).value();
                handlerMapping.put(baseUrl+url, method);
                try {
                    controllerMap.put(baseUrl+url, cla.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void loadClass() {
        for(String className : classNames){
            try {
                Class<?> cla = Class.forName(className);
                if(cla.isAnnotationPresent(MyController.class)){
                    ioc.put(cla.getName(), cla.newInstance());
                }
                else{
                    continue;
                }
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


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = req.getRequestURI().replace(req.getContextPath(), "");
        if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!");
        }

        Method method = handlerMapping.get(url);

        Class<?>[] paramTypes = method.getParameterTypes();
        Map<String, String[]> paramMap = req.getParameterMap();

        Object[] paramValues = new Object[paramTypes.length];

        for(int i=0; i<paramTypes.length; i++){
            String requestParam = paramTypes[i].getSimpleName();

            if(requestParam.equals("HttpServletRequest")){
                paramValues[i] = req;
                continue;
            }
            if(requestParam.equals("HttpServletResponse")){
                paramValues[i] = resp;
                continue;
            }

            for(String[] param:paramMap.values()){
                String t = Arrays.toString(param);
                paramValues[i] = t;
                i++;
            }
        }

        try {
            method.invoke(controllerMap.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}