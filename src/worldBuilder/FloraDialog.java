package worldBuilder;

/**
 * Dialog to enable to populate MeshPoint floral attributes
 */
public class FloraDialog extends ResourceDialog {
	private static final String[] floraClasses = {"Barren", "Grass", "Brush", "Tree"};
	private Parameters parms;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public FloraDialog(Map map)  {
		super(map, AttributeEngine.WhichMap.FLORA, floraClasses, map.floraNames);
		setTitle("Flora Placement");
		
		// plug in flora-specific default values
		parms = Parameters.getInstance();
		rsrc_rules.setText(parms.flora_rules);
		rsrc_pct.setValue(parms.dFloraPct);
		rsrc_3.setValue(parms.dFloraMin);
		rsrc_3.setUpperValue(parms.dFloraMax);
	}
	
	protected void accept() {
		// remember the chosen rules file and percentages
		parms.flora_rules = rsrc_rules.getText();
		parms.dFloraPct = rsrc_pct.getValue();
		parms.dFloraMin = rsrc_3.getValue();
		parms.dFloraMax = rsrc_3.getUpperValue();
		
		super.accept();	// let superclass handle the updates
	}
}
