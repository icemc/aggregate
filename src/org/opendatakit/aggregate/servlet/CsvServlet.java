/*
 * Copyright (C) 2009 Google Inc. 
 * Copyright (C) 2010 University of Washington.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendatakit.aggregate.CallingContext;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.form.PersistentResults;
import org.opendatakit.aggregate.form.PersistentResults.ResultType;
import org.opendatakit.aggregate.task.CsvGenerator;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;

/**
 * Servlet to generate a CSV file for download
 * 
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class CsvServlet extends ServletUtilBase {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = 1533921429476018375L;

  /**
   * URI from base
   */
  public static final String ADDR = "view/csv";

  /**
   * Handler for HTTP Get request that responds with CSV
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	CallingContext cc = ContextFactory.getCallingContext(this, req);

    // get parameter
    String formId = getParameter(req, ServletConsts.FORM_ID);

    if (formId == null) {
      errorMissingKeyParam(resp);
      return;
    }

    Form form = null;
    try {
      form = Form.retrieveForm(formId, cc);
    } catch (ODKFormNotFoundException e1) {
      odkIdNotFoundError(resp);
      return;
    }

    CsvGenerator generator = (CsvGenerator) cc.getBean(BeanDefs.CSV_BEAN);
    try {
        PersistentResults r = new PersistentResults( ResultType.CSV, form, null, cc);
        r.persist(cc);
    	CallingContext ccDaemon = ContextFactory.getCallingContext(this, req);
    	ccDaemon.setAsDaemon(true);
		generator.createCsvTask(form, r.getSubmissionKey(), 1L, ccDaemon);
	} catch (ODKDatastoreException e) {
		e.printStackTrace();
		resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		return;
	}
    resp.sendRedirect(cc.getWebApplicationURL(ResultServlet.ADDR));
  }
}
