package aspects;

import java.util.logging.*;

import org.aspectj.lang.Signature;

aspect Trace{

        private static Logger logger = Logger.getLogger("Tracing");

        public Trace(){

        try{
                FileHandler handler=new FileHandler("trace.log",false);

                logger.addHandler(handler);

                handler.setFormatter(new Formatter(){

                        public String format(LogRecord record){
                                return record.getMessage()+"\n";
                                }
                                });
                        }

        catch(Exception e){}

        }

        pointcut traceMethods() : (execution(* *(..))&& !cflow(within(Trace)));

        before(): traceMethods(){
                Signature sig = thisJoinPointStaticPart.getSignature();
                String line =""+ thisJoinPointStaticPart.getSourceLocation().getLine();
                String sourceName = thisJoinPointStaticPart.getSourceLocation().getWithinType().getCanonicalName();
                Logger.getLogger("Tracing").log(
                        Level.INFO,
                        "Call from "
                        +  sourceName
                        +" line " +
                        line
                        +" to " +sig.getDeclaringTypeName() + "." + sig.getName()
                );
        }
}
