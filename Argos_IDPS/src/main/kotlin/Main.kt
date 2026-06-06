package ao.argosidps

import ao.argosidps.ai.loadModel
import ao.argosidps.capture.startCapture
import ao.argosidps.colors.GREEN
import ao.argosidps.colors.RED
import ao.argosidps.colors.RESET
import ao.argosidps.configurations.Configuration
import ao.argosidps.display.printTableHeader
import ao.argosidps.display.startKeyboardController
import ao.argosidps.privileges.isRoot
import ao.argosidps.smtp.sendAlert
import kotlin.system.exitProcess

suspend fun main() {
    /*val processBuilder = ProcessBuilder()
    val process = processBuilder.command(
        AIModelProperties.PYTHON_INTERPRETER,
        AIModelProperties.PYTHON_SCRIPT
    ).start()*/
    if (!isRoot()) {
        println("${RED}root permission required!${RESET}\n")
        exitProcess(1)
    }
    println("""$GREEN
      >>       >======>        >===>        >===>        >=>>=>   
     >>=>      >=>    >=>    >>    >=>    >=>    >=>   >=>    >=> 
    >> >=>     >=>    >=>   >=>         >=>        >=>  >=>       
   >=>  >=>    >> >==>      >=>         >=>        >=>    >=>     
  >=====>>=>   >=>  >=>     >=>   >===> >=>        >=>       >=>  
 >=>      >=>  >=>    >=>    >=>    >>    >=>     >=>  >=>    >=> 
>=>        >=> >=>      >=>   >====>        >===>        >=>>=>   
                                                                      
        |$RESET
    """.trimMargin())
    //sendAlert("", ""); exitProcess(0)
    loadModel()
    
    // Start keyboard controller for interactive controls
    startKeyboardController()
    
    // Print table header before starting capture
    printTableHeader()
    
    startCapture()
    //val output = process.inputStream.bufferedReader().readText()
    //println(output)
}