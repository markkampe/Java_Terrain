package worldBuilder;

/**
 * Dialog to enable to populate MeshPoint floral attributes
 */
public class FaunaDialog extends ResourceDialog {
	private static final String[] faunaClasses = {"None", "Birds", "Small Game", "Large Game"};

	private Parameters parms;

	private static final long serialVersionUID = 1L;

	/**
	 * instantiate the widgets and register the listeners
	 */
	public FaunaDialog(Map map)  {
		super(map, AttributeEngine.WhichMap.FAUNA, faunaClasses, map.faunaNames);
		setTitle("Fauna Placement");
		
		// plug in fauna-specific default values
		parms = Parameters.getInstance();
		rsrc_rules.setText(parms.fauna_rules);
		rsrc_pct.setValue(parms.dFaunaPct);
		rsrc_3.setValue(parms.dFaunaMin);
		rsrc_3.setUpperValue(parms.dFaunaMax);
	}

	protected void accept() {
		// remember the chosen rules file and percentages
		parms.fauna_rules = rsrc_rules.getText();
		parms.dFaunaPct = rsrc_pct.getValue();
		parms.dFaunaMin = rsrc_3.getValue();
		parms.dFaunaMax = rsrc_3.getUpperValue();

		super.accept();	// let superclass handle the updates
	}
}

