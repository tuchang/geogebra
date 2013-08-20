package geogebra.common.move.ggtapi.operations;

import geogebra.common.move.ggtapi.models.AuthenticationModel;
import geogebra.common.move.ggtapi.models.json.JSONObject;
import geogebra.common.move.ggtapi.models.json.JSONString;
import geogebra.common.move.ggtapi.views.LoginView;
import geogebra.common.move.operations.BaseOperation;
import geogebra.common.move.views.SuccessErrorRenderable;

/**
 * @author gabor
 * 
 * Operational class for login functionality
 *
 */
public class LoginOperation extends BaseOperation<SuccessErrorRenderable> {
	
	/**
	 * Creates a new Login operation
	 */
	public LoginOperation()  {
		
	}

	/**
	 * @param response from GGT
	 * 
	 * Successfull login operation
	 * 
	 */
	public void loginSuccess(JSONObject response) {
		getModel().loginSuccess(response);
		getView().loginSuccess(response);
	}

	/**
	 * @param response from GGT
	 * 
	 * Error happened during login
	 */
	public void loginError(JSONObject response) {
		getModel().loginError(response);
		getView().loginError(response);
	}
	/**
	 * @param response from GGT
	 * 
	 * Error happened during login
	 */
	public void loginResult(JSONObject response){
		if (response.get("error") == null) {
			loginSuccess(response);
		} else {
			loginError(response);
		}
	}
	
	@Override
	public AuthenticationModel getModel() {
		return (AuthenticationModel) super.getModel();
	}
	
	@Override
	public LoginView getView() {
		return (LoginView) super.getView();
	}

	/**
	 * @return JSONObject containing the stored login data
	 */
	public JSONObject getStoredLoginData() {
		return getModel().getStoredLoginData();
	}

	/**
	 * @return the user name from the storage
	 */
	public String getUserName() {
		return ((JSONString) getModel().getStoredLoginData().get(AuthenticationModel.GGB_LOGIN_DATA_USERNAME_KEY_NAME)).stringValue();
	}

	/**
	 * @return boolean indicating that the user is already logged in.
	 */
	public boolean isLoggedIn() {
		return getModel().getLoginToken() != null;
	}

}
