/*
*
* Paros and its related class files.
* 
* Paros is an HTTP/HTTPS proxy for assessing web application security.
* Copyright (C) 2003-2004 Chinotec Technologies Company
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the Clarified Artistic License
* as published by the Free Software Foundation.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* Clarified Artistic License for more details.
* 
* You should have received a copy of the Clarified Artistic License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
// ZAP: 2011/12/04 Support deleting alerts
// ZAP: 2012/01/02 Separate param and attack
// ZAP: 2012/01/23 Changed the method compareTo to compare the fields correctly
// with each other.
// ZAP: 2012/03/15 Changed the methods toPluginXML and getUrlParamXML to use the class
// StringBuilder instead of StringBuffer and replaced some string concatenations with
// calls to the method append of the class StringBuilder.
// ZAP: 2012/04/25 Added @Override annotation to all appropriate methods.
// ZAP: 2012/05/02 Changed to not create a new String in the setters.
// ZAP: 2012/07/10 Issue 323: Added getIconUrl()
// ZAP: 2012/10/08 Issue 391: Performance improvements
// ZAP: 2012/12/19 Code Cleanup: Moved array brackets from variable name to type

package org.parosproxy.paros.core.scanner;

import java.net.URL;
import java.sql.SQLException;

import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.db.RecordAlert;
import org.parosproxy.paros.extension.report.ReportGenerator;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;


public class Alert implements Comparable<Object>  {

	public static final int RISK_INFO 	= 0;
	public static final int RISK_LOW 	= 1;
	public static final int RISK_MEDIUM = 2;
	public static final int RISK_HIGH 	= 3;

	// ZAP: Added FALSE_POSITIVE
	public static final int FALSE_POSITIVE = 0;
	public static final int SUSPICIOUS = 1;
	public static final int WARNING = 2;
	
	public static final String[] MSG_RISK = {"Informational", "Low", "Medium", "High"};
	// ZAP: Added "false positive"
	public static final String[] MSG_RELIABILITY = {"False Positive", "Suspicious", "Warning"};
	
	private int		alertId = -1;	// ZAP: Changed default alertId
	private int		pluginId = 0;
	private String 	alert = "";
	private int risk = RISK_INFO;
	private int reliability = WARNING;
	private String 	description = "";
	private String 	uri = "";
	private String 	param = "";
	private String 	attack = "";
	private String 	otherInfo = "";
	private String 	solution = "";
	private String	reference = "";
	// Tempory ref - should be cleared asap after use
	private HttpMessage message = null;
	// ZAP: Added sourceHistoryId to Alert
	private int		sourceHistoryId = 0;
	private HistoryReference historyRef = null;
	// ZAP: Added logger
	Logger logger = Logger.getLogger(Alert.class);
	// Cache this info so that we dont have to keep a ref to the HttpMessage
	private String method = null;
	private String postData;
	private URI msgUri = null;
	
	public Alert(int pluginId) {
		this.pluginId = pluginId;
		
	}
	
	public Alert(int pluginId, int risk, int reliability, String alert) {
		this(pluginId);
		setRiskReliability(risk, reliability);
		setAlert(alert);
	}

	public Alert(RecordAlert recordAlert) {
	    this(recordAlert.getPluginId(), recordAlert.getRisk(), recordAlert.getReliability(), recordAlert.getAlert());
	    // ZAP: Set the alertId
	    this.alertId = recordAlert.getAlertId();
        try {
        	historyRef = new HistoryReference(recordAlert.getHistoryId());
            setDetail(recordAlert.getDescription(), recordAlert.getUri(), 
            		recordAlert.getParam(), recordAlert.getAttack(), recordAlert.getOtherInfo(), 
            		recordAlert.getSolution(), recordAlert.getReference(), 
            		historyRef.getHttpMessage());
            // ZAP: Set up the Alert History Id

        } catch (HttpMalformedHeaderException e) {
        	// ZAP: Just an indication the history record doesnt exist
        	logger.debug(e.getMessage(), e);
        } catch (Exception e) {
        	// ZAP: Log the exception
        	logger.error(e.getMessage(), e);
        }
	    
	}
	
	public Alert(RecordAlert recordAlert, HistoryReference ref) {
	    this(recordAlert.getPluginId(), recordAlert.getRisk(), recordAlert.getReliability(), recordAlert.getAlert());
	    // ZAP: Set the alertId
	    this.alertId = recordAlert.getAlertId();
		historyRef = ref;
        try {
            setDetail(recordAlert.getDescription(), recordAlert.getUri(), 
            		recordAlert.getParam(), recordAlert.getAttack(), recordAlert.getOtherInfo(), 
            		recordAlert.getSolution(), recordAlert.getReference(), 
            		ref == null ? null : ref.getHttpMessage());
        } catch (Exception e) {
        	// ZAP: Log the exception
        	logger.error(e.getMessage(), e);
        }
	}

	public void setRiskReliability(int risk, int reliability) {
		this.risk = risk;
		this.reliability = reliability;
	}
	
	public void setAlert(String alert) {
	    if (alert == null) return;
	    // ZAP: Changed to not create a new String.
	    this.alert = alert;
	}
	
	

	public void setDetail(String description, String uri, String param, String attack, String otherInfo, 
			String solution, String reference, HttpMessage msg) {
		setDescription(description);
		setUri(uri);
		setParam(param);
		setAttack(attack);
		setOtherInfo(otherInfo);
		setSolution(solution);
		setReference(reference);
		setMessage(msg);
		if (msg != null) {
			this.historyRef = msg.getHistoryRef();
		}
	}

	private void setDetail(String description, String uri, String param, String attack, String otherInfo, 
			String solution, String reference, HistoryReference href) {
		setDescription(description);
		setUri(uri);
		setParam(param);
		setAttack(attack);
		setOtherInfo(otherInfo);
		setSolution(solution);
		setReference(reference);
		this.historyRef = href;
	}

	public void setUri(String uri) {
    	// ZAP: Cope with null
	    if (uri == null) return;
	    // ZAP: Changed to not create a new String.
		this.uri = uri;
	}
	
	
	public void setDescription(String description) {
	    if (description == null) return;
	    // ZAP: Changed to not create a new String.
		this.description = description;
	}
	
	public void setParam(String param) {
	    if (param == null) return;
	    // ZAP: Changed to not create a new String.
		this.param = param;
	}
	
	
	public void setOtherInfo(String otherInfo) {
	    if (otherInfo == null) return;
	    // ZAP: Changed to not create a new String.
		this.otherInfo = otherInfo;
	}

	public void setSolution(String solution) {
	    if (solution == null) return;
	    // ZAP: Changed to not create a new String.
		this.solution = solution;
	}

	public void setReference(String reference) {
	    if (reference == null) return;
	    // ZAP: Changed to not create a new String.
		this.reference = reference;
	}

	public void setMessage(HttpMessage message) {
	    if (message != null) {
	    	this.message = message;
	    	this.method = message.getRequestHeader().getMethod();
	    	this.postData = message.getRequestBody().toString();
	    	this.msgUri = message.getRequestHeader().getURI();
	    } else {
	    	// Used to clear the ref so we dont hold onto it
	    	this.message = null;
	    }
	}
	
	@Override
	public int compareTo(Object o2) throws ClassCastException {
		Alert alert2 = (Alert) o2;
		
		if (risk < alert2.risk) {
			return -1;
		} else if (risk > alert2.risk) {
			return 1;
		}
		
		if (reliability < alert2.reliability) {
			return -1;
		} else if (reliability > alert2.reliability) {
			return 1;
		}
		
		int result = alert.compareToIgnoreCase(alert2.alert);
		if (result != 0) {
			return result;
		}
		
		// ZAP: changed to compare the field uri with alert2.uri
		result = uri.compareToIgnoreCase(alert2.uri);
		if (result != 0) {
			return result;
		}
		
		// ZAP: changed to compare the field param with alert2.param
		result = param.compareToIgnoreCase(alert2.param);
		if (result != 0) {
			return result;
		}
		
		return otherInfo.compareToIgnoreCase(alert2.otherInfo);
	} 


	/**
	Override equals.  Alerts are equal if the alert, uri and param is the same.
	*/
	@Override
	public boolean equals(Object obj) {
		Alert item = null;
		if (obj instanceof Alert) {
			item = (Alert) obj;
			if ((pluginId == item.pluginId) && uri.equalsIgnoreCase(item.uri)
				&& param.equalsIgnoreCase(item.param) && otherInfo.equalsIgnoreCase(item.otherInfo)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	Create a new instance of AlertItem with same members.
	*/
	public Alert newInstance() {
		Alert item = new Alert(this.pluginId);
		item.setRiskReliability(this.risk, this.reliability);
		item.setAlert(this.alert);
		item.setDetail(this.description, this.uri, this.param, this.attack, this.otherInfo, this.solution, this.reference, this.historyRef);
		return item;
	}
	
	public String toPluginXML(String urls) {
		StringBuilder sb = new StringBuilder(150); // ZAP: Changed the type to StringBuilder.
		sb.append("<alertitem>\r\n");
		sb.append("  <pluginid>").append(pluginId).append("</pluginid>\r\n");
		sb.append("  <alert>").append(alert).append("</alert>\r\n");
		sb.append("  <riskcode>").append(risk).append("</riskcode>\r\n");
		sb.append("  <reliability>").append(reliability).append("</reliability>\r\n");
		sb.append("  <riskdesc>").append(replaceEntity(MSG_RISK[risk] + " (" + MSG_RELIABILITY[reliability] + ")")).append("</riskdesc>\r\n");
        sb.append("  <desc>").append(paragraph(replaceEntity(description))).append("</desc>\r\n");

        sb.append(urls);

        sb.append("  <solution>").append(paragraph(replaceEntity(solution))).append("</solution>\r\n");
        // ZAP: Added otherInfo to the report
        if (otherInfo != null && otherInfo.length() > 0) {
            sb.append("  <otherinfo>").append(paragraph(replaceEntity(otherInfo))).append("</otherinfo>\r\n");
        }
		sb.append("  <reference>" ).append(paragraph(replaceEntity(reference))).append("</reference>\r\n");
		
		sb.append("</alertitem>\r\n");
		return sb.toString();
	}
   
	public String replaceEntity(String text) {
		String result = null;
		if (text != null) {
			result = ReportGenerator.entityEncode(text);
		}
		return result;
	}
	
	public String paragraph(String text) {
		String result = null;
		result = "<p>" + text.replaceAll("\\r\\n","</p><p>").replaceAll("\\n","</p><p>") + "</p>";
        result = result.replaceAll("&lt;ul&gt;", "<ul>").replaceAll("&lt;/ul&gt;", "</ul>").replaceAll("&lt;li&gt;", "<li>").replaceAll("&lt;/li&gt;", "</li>");
        //result = text.replaceAll("\\r\\n","<br/>").replaceAll("\\n","<br/>");

        return result;
	}
    
    private String breakNoSpaceString(String text) {
        String result = null;
        if (text != null) {
        	result = text.replaceAll("&amp;","&amp;<wbr/>");
        }
        return result;
        
    }
		
    /**
     * @return Returns the alert.
     */
    public String getAlert() {
        return alert;
    }
    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }
    /**
     * @return Returns the id.
     */
    public int getPluginId() {
        return pluginId;
    }
    /**
     * @return Returns the message.
     */
    public HttpMessage getMessage() {
    	if (this.message != null) {
    		return this.message;
    	}
    	if (this.historyRef != null) {
    		try {
				return this.historyRef.getHttpMessage();
			} catch (HttpMalformedHeaderException | SQLException e) {
	        	logger.error(e.getMessage(), e);
			}
    	}
        return null;
    }
    /**
     * @return Returns the otherInfo.
     */
    public String getOtherInfo() {
        return otherInfo;
    }
    /**
     * @return Returns the param.
     */
    public String getParam() {
        return param;
    }
    /**
     * @return Returns the reference.
     */
    public String getReference() {
        return reference;
    }
    /**
     * @return Returns the reliability.
     */
    public int getReliability() {
        return reliability;
    }
    /**
     * @return Returns the risk.
     */
    public int getRisk() {
        return risk;
    }
    
    public URL getIconUrl() {
    	if (reliability == Alert.FALSE_POSITIVE) {
    		// Special case - theres no risk - use the green flag
			return Constant.OK_FLAG_IMAGE_URL;
    	}

    	switch (risk) {
    	case Alert.RISK_INFO:
    		return Constant.INFO_FLAG_IMAGE_URL;
    	case Alert.RISK_LOW:
    		return Constant.LOW_FLAG_IMAGE_URL;
    	case Alert.RISK_MEDIUM:
    		return Constant.MED_FLAG_IMAGE_URL;
    	case Alert.RISK_HIGH:
    		return Constant.HIGH_FLAG_IMAGE_URL;
    	}
    	return null;
    }
    /**
     * @return Returns the solution.
     */
    public String getSolution() {
        return solution;
    }
    /**
     * @return Returns the uri.
     */
    public String getUri() {
        return uri;
    }
    /**
     * @return Returns the alertId.
     */
    public int getAlertId() {
        return alertId;
    }
    /**
     * @param alertId The alertId to set.
     */
    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }
    
    public String getUrlParamXML() {
    	StringBuilder sb = new StringBuilder(200); // ZAP: Changed the type to StringBuilder.
        sb.append("  <uri>").append(breakNoSpaceString(replaceEntity(uri))).append("</uri>\r\n");
        sb.append("  <param>").append(breakNoSpaceString(replaceEntity(param))).append("</param>\r\n");
        sb.append("  <attack>").append(breakNoSpaceString(replaceEntity(attack))).append("</attack>\r\n");
        sb.append("  <otherinfo>").append(breakNoSpaceString(replaceEntity(otherInfo))).append("</otherinfo>\r\n");
        return sb.toString();
    }

	public int getSourceHistoryId() {
		return sourceHistoryId;
	}

	public void setSourceHistoryId(int sourceHistoryId) {
		this.sourceHistoryId = sourceHistoryId;
	}
	
	public HistoryReference getHistoryRef () {
		return this.historyRef;
	}

	public void setHistoryRef(HistoryReference historyRef) {
		this.historyRef = historyRef;
		if (historyRef != null) {
			this.sourceHistoryId = historyRef.getHistoryId();
		}
	}

	public String getAttack() {
		return attack;
	}

	public void setAttack(String attack) {
		this.attack = attack;
	}

	public String getMethod() {
		return method;
	}

	public String getPostData() {
		return postData;
	}

	public URI getMsgUri() {
		return msgUri;
	}
    
}	


