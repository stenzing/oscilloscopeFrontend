package sg.osc;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.controlsfx.control.StatusBar;
import javafx.event.*;
import javafx.scene.layout.*;
import javafx.scene.chart.*;
import javafx.stage.Stage;

import jssc.SerialPortList;
import jssc.SerialPort; 
import jssc.SerialPortException;
import jssc.SerialPortEvent; 
import jssc.SerialPortEventListener; 
import java.util.*;
 
public class Oscilloscope extends Application {
    private static final List<String> comPorts = new ArrayList<>();
    private XYChart.Series sr = new XYChart.Series();
    private SerialPort serialPort = null;
    private StatusBar statusBar = null;

    public static void main(String[] args) {
	List<String> ports = getAvailablePorts();
	ports.forEach(p -> comPorts.add(p));
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
      primaryStage.setTitle("Oscilloscope 1.0.0");
      VBox p = new VBox();

      MenuBar mb = new MenuBar();
      mb.getMenus().add(getMenu(comPorts));
      statusBar = new StatusBar();
      p.getChildren().add(mb);
      p.getChildren().add(getChart());
      p.getChildren().add(statusBar);
      VBox v = new VBox(p);
      Scene scene = new Scene(v, 400, 200); 
      primaryStage.setScene(scene);

      primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
	if (serialPort != null) {
	    try {
	      statusBar.setText("Closing Port");
	      serialPort.closePort();
	    } catch (SerialPortException ex) {
              System.out.println(ex);
            } finally {
              serialPort = null;
	    }
	}
    }

    private void onPortSelect(String port) {
      try {
        stop();
        sr.setName(port);
        serialPort = new SerialPort(port);
        statusBar.setText("Opening port: "+ port);
        serialPort.openPort();//Open serial port
        serialPort.setParams(SerialPort.BAUDRATE_115200, 
                             SerialPort.DATABITS_8,
                             SerialPort.STOPBITS_1,
                             SerialPort.PARITY_NONE);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);
        int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;//Prepare mask
        serialPort.setEventsMask(mask);//Set mask
	serialPort.addEventListener(new SerialPortReader(sr));
        //serialPort.writeBytes("This is a test string".getBytes());//Write data to port
      } catch (Exception ex) {
          System.out.println(ex);
	  try {
            stop();//Close serial port
	  } catch (Exception ex2) {
	    System.out.println(ex);
	  }
	  sr.setName("NONE");
      }
    }

    private Menu getMenu(List<String> comPorts) {
      Menu mn = new Menu("Port");
      ToggleGroup t1 = new ToggleGroup();
      comPorts.forEach(p -> {
          RadioMenuItem mi = new RadioMenuItem(p);
	  mi.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
	      onPortSelect(p);
	    }
	  });
	  mi.setToggleGroup(t1);
          mn.getItems().add(mi);
      });
      return mn;
    }

    private LineChart getChart() {
      //x axis representation
      NumberAxis x = new NumberAxis();
      x.setLabel("Time");
      //y axis representation
      NumberAxis y = new NumberAxis();
      y.setLabel("Voltage");

      sr = new XYChart.Series();
      LineChart ll = new LineChart(x, y);
      ll.getData().add(sr);

      return ll;
    }

    static class SerialPortReader implements SerialPortEventListener {
	private final XYChart.Series dataSeries;

	public SerialPortReader(XYChart.Series dataSeries) {
	    this.dataSeries = dataSeries;
	}

        public void serialEvent(SerialPortEvent event) {
	    if(event.isRXCHAR()){//If data is available
		dataSeries.getData().clear();
	        dataSeries.getData().add(new XYChart.Data( 1, 567));
		
        	if(event.getEventValue() == 10){//Check bytes count in the input buffer
            	    //Read data, if 10 bytes available 
            	    /*try {
                	byte buffer[] = serialPort.readBytes(10);
            	    } catch (SerialPortException ex) {
                	System.out.println(ex);
            	    }*/
        	}
    	    } else if(event.isCTS()){//If CTS line has changed state
	        if(event.getEventValue() == 1){//If line is ON
    	        System.out.println("CTS - ON");
    	        } else {
                System.out.println("CTS - OFF");
        	}
    	    } else if(event.isDSR()){///If DSR line has changed state
        	if(event.getEventValue() == 1){//If line is ON
            	    System.out.println("DSR - ON");
        	} else {
            	    System.out.println("DSR - OFF");
        	}
    	    }
	}
    }

    private static List<String> getAvailablePorts() {
	String[] portNames = SerialPortList.getPortNames();
	List<String> results = new ArrayList<>();
	for(int i = 0; i < portNames.length; i++){
            results.add(portNames[i]);
        }
        return results;
    }
}
