package worldBuilder;

/**
 * Dialog to enable to populate MeshPoint Mineral attributes
 */
public class MineralDialog extends ResourceDialog {
	private static final String[] rockClasses = {"None", "Stone", "Metal", "Precious"};
	
	private Parameters parms;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public MineralDialog(Map map)  {
		super(map, AttributeEngine.WhichMap.MINERAL, rockClasses, map.getRockNames());
		setTitle("Mineral Placement");

		// plug in Mineral-specific default values
		parms = Parameters.getInstance();
		rsrc_rules.setText(parms.mineral_rules);
		rsrc_pct.setValue(parms.dRockPct);
		rsrc_3.setValue(parms.dRockMin);
		rsrc_3.setUpperValue(parms.dRockMax);
	}

	protected void accept() {
		// remember the chosen rules file and percentages
		parms.mineral_rules = rsrc_rules.getText();
		parms.dRockPct = rsrc_pct.getValue();
		parms.dRockMin = rsrc_3.getValue();
		parms.dRockMax = rsrc_3.getUpperValue();

		super.accept();	// let superclass handle the updates
	}
}
