package sjtu.scuc.academic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class Main {

    private static Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) {


        Doit di=new Doit();
        di.mainprocess();

    }

}
