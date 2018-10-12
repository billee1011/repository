package com.cai.game.util;

public class TableManager {
	private static final TableManager manager = new TableManager();

	private MapWrapper[] eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] feng_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] feng_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] ph_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] ph_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] ph_feng_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] ph_feng_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] ewb_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] ewb_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] ewb_feng_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] feng_chi_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] feng_chi_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] feng_chi_hd_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] feng_chi_hd_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] feng_chi_dfl_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] feng_chi_dfl_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] yao_jiu_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] yao_jiu_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private MapWrapper[] ji_xian_eye_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];
	private MapWrapper[] ji_xian_table = new MapWrapper[GameUtilConstants.TABLE_COUNT];

	private TableManager() {
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			eye_table[i] = new MapWrapper();
			table[i] = new MapWrapper();
			feng_eye_table[i] = new MapWrapper();
			feng_table[i] = new MapWrapper();

			ph_eye_table[i] = new MapWrapper();
			ph_table[i] = new MapWrapper();
			ph_feng_eye_table[i] = new MapWrapper();
			ph_feng_table[i] = new MapWrapper();

			ewb_eye_table[i] = new MapWrapper();
			ewb_table[i] = new MapWrapper();
			ewb_feng_table[i] = new MapWrapper();

			feng_chi_eye_table[i] = new MapWrapper();
			feng_chi_table[i] = new MapWrapper();

			feng_chi_dfl_eye_table[i] = new MapWrapper();
			feng_chi_dfl_table[i] = new MapWrapper();

			yao_jiu_eye_table[i] = new MapWrapper();
			yao_jiu_table[i] = new MapWrapper();

			ji_xian_eye_table[i] = new MapWrapper();
			ji_xian_table[i] = new MapWrapper();

			feng_chi_hd_eye_table[i] = new MapWrapper();
			feng_chi_hd_table[i] = new MapWrapper();
		}
	}

	public static TableManager getInstance() {
		return manager;
	}

	protected boolean contains_feng_chi_dfl(int key, int magic_count, boolean has_eye, boolean is_wan_tiao_tong, boolean check_peng_hu,
			boolean check_258, boolean check_feng_chi) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return false;
		}

		MapWrapper map = null;

		if (check_peng_hu) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = ph_eye_table[magic_count];
				} else {
					map = ph_table[magic_count];
				}
			} else {
				if (has_eye) {
					map = ph_feng_eye_table[magic_count];
				} else {
					map = ph_feng_table[magic_count];
				}
			}
		} else if (check_258) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = ewb_eye_table[magic_count];
				} else {
					map = ewb_table[magic_count];
				}
			} else {
				if (has_eye) {
					return false;
				} else {
					map = ewb_feng_table[magic_count];
				}
			}
		} else if (check_feng_chi) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = eye_table[magic_count];
				} else {
					map = table[magic_count];
				}
			} else {
				if (has_eye) {
					map = feng_chi_dfl_eye_table[magic_count];
				} else {
					map = feng_chi_dfl_table[magic_count];
				}
			}
		} else {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = eye_table[magic_count];
				} else {
					map = table[magic_count];
				}
			} else {
				if (has_eye) {
					map = feng_eye_table[magic_count];
				} else {
					map = feng_table[magic_count];
				}
			}
		}

		return map.contains(key);
	}

	protected boolean contains(int key, int magic_count, boolean has_eye, boolean is_wan_tiao_tong, boolean check_peng_hu, boolean check_258,
			boolean check_feng_chi) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return false;
		}

		MapWrapper map = null;

		if (check_peng_hu) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = ph_eye_table[magic_count];
				} else {
					map = ph_table[magic_count];
				}
			} else {
				if (has_eye) {
					map = ph_feng_eye_table[magic_count];
				} else {
					map = ph_feng_table[magic_count];
				}
			}
		} else if (check_258) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = ewb_eye_table[magic_count];
				} else {
					map = ewb_table[magic_count];
				}
			} else {
				if (has_eye) {
					return false;
				} else {
					map = ewb_feng_table[magic_count];
				}
			}
		} else if (check_feng_chi) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = eye_table[magic_count];
				} else {
					map = table[magic_count];
				}
			} else {
				if (has_eye) {
					map = feng_chi_eye_table[magic_count];
				} else {
					map = feng_chi_table[magic_count];
				}
			}
		} else {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = eye_table[magic_count];
				} else {
					map = table[magic_count];
				}
			} else {
				if (has_eye) {
					map = feng_eye_table[magic_count];
				} else {
					map = feng_table[magic_count];
				}
			}
		}

		return map.contains(key);
	}

	protected boolean contains_hd(int key, int magic_count, boolean has_eye) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return false;
		}

		MapWrapper map = null;

		if (has_eye) {
			map = feng_chi_hd_eye_table[magic_count];
		} else {
			map = feng_chi_hd_table[magic_count];
		}

		return map.contains(key);
	}

	protected boolean contains_yao_jiu(int key, int magic_count, boolean has_eye) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return false;
		}

		MapWrapper map = null;

		if (has_eye) {
			map = yao_jiu_eye_table[magic_count];
		} else {
			map = yao_jiu_table[magic_count];
		}

		return map.contains(key);
	}

	protected boolean contains_ji_xian(int key, int magic_count, boolean has_eye, boolean is_wan_tiao_tong) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return false;
		}

		MapWrapper map = null;

		if (is_wan_tiao_tong) {
			if (has_eye) {
				map = ji_xian_eye_table[magic_count];
			} else {
				map = ji_xian_table[magic_count];
			}
		} else {
			if (has_eye) {
				map = feng_chi_eye_table[magic_count];
			} else {
				map = feng_chi_table[magic_count];
			}
		}

		return map.contains(key);
	}

	protected void put_feng_chi_dfl(int key, int magic_count, boolean has_eye) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return;
		}

		MapWrapper map = null;

		if (has_eye) {
			map = feng_chi_dfl_eye_table[magic_count];
		} else {
			map = feng_chi_dfl_table[magic_count];
		}

		map.put(key);
	}

	protected void put_ji_xian(int key, int magic_count, boolean has_eye) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return;
		}

		MapWrapper map = null;

		if (has_eye) {
			map = ji_xian_eye_table[magic_count];
		} else {
			map = ji_xian_table[magic_count];
		}

		map.put(key);
	}

	protected void put_feng_chi_hd(int key, int magic_count, boolean has_eye) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return;
		}

		MapWrapper map = null;

		if (has_eye) {
			map = feng_chi_hd_eye_table[magic_count];
		} else {
			map = feng_chi_hd_table[magic_count];
		}

		map.put(key);
	}

	protected void put(int key, int magic_count, boolean has_eye, boolean is_wan_tiao_tong, boolean check_peng_hu, boolean check_258,
			boolean check_feng_chi) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return;
		}

		MapWrapper map = null;

		if (check_peng_hu) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = ph_eye_table[magic_count];
				} else {
					map = ph_table[magic_count];
				}
			} else {
				if (has_eye) {
					map = ph_feng_eye_table[magic_count];
				} else {
					map = ph_feng_table[magic_count];
				}
			}
		} else if (check_258) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = ewb_eye_table[magic_count];
				} else {
					map = ewb_table[magic_count];
				}
			} else {
				if (has_eye) {
					return;
				} else {
					map = ewb_feng_table[magic_count];
				}
			}
		} else if (check_feng_chi) {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = eye_table[magic_count];
				} else {
					map = table[magic_count];
				}
			} else {
				if (has_eye) {
					map = feng_chi_eye_table[magic_count];
				} else {
					map = feng_chi_table[magic_count];
				}
			}
		} else {
			if (is_wan_tiao_tong) {
				if (has_eye) {
					map = eye_table[magic_count];
				} else {
					map = table[magic_count];
				}
			} else {
				if (has_eye) {
					map = feng_eye_table[magic_count];
				} else {
					map = feng_table[magic_count];
				}
			}
		}

		map.put(key);
	}

	protected void put_yao_jiu(int key, int magic_count, boolean has_eye) {
		if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
			// System.err.println("王牌数目不能小于0或大于8");
			return;
		}

		MapWrapper map = null;

		if (has_eye) {
			map = yao_jiu_eye_table[magic_count];
		} else {
			map = yao_jiu_table[magic_count];
		}

		map.put(key);
	}

	protected void dump() {
		String path1 = "tbl_mj/table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_feng() {
		String path1 = "tbl_mj/feng_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/feng_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_ph() {
		String path1 = "tbl_mj/ph_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ph_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/ph_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ph_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_ph_feng() {
		String path1 = "tbl_mj/ph_feng_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ph_feng_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/ph_feng_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ph_feng_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_ewb() {
		String path1 = "tbl_mj/ewb_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ewb_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/ewb_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ewb_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_ewb_feng() {
		String path1 = "tbl_mj/ewb_feng_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ewb_feng_table[i].dump(path1 + i + ".tbl");
		}
	}

	protected void dump_feng_chi() {
		String path1 = "tbl_mj/feng_chi_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_chi_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/feng_chi_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_chi_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_feng_chi_hd() {
		String path1 = "tbl_mj/feng_chi_hd_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_chi_hd_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/feng_chi_hd_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_chi_hd_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_feng_chi_dfl() {
		String path1 = "tbl_mj/feng_chi_dfl_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_chi_dfl_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/feng_chi_dfl_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			feng_chi_dfl_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_yao_jiu() {
		String path1 = "tbl_mj/yao_jiu_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			yao_jiu_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/yao_jiu_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			yao_jiu_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	protected void dump_ji_xian() {
		String path1 = "tbl_mj/ji_xian_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ji_xian_table[i].dump(path1 + i + ".tbl");
		}
		String path2 = "tbl_mj/ji_xian_eye_table_";
		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			ji_xian_eye_table[i].dump(path2 + i + ".tbl");
		}
	}

	public void load() {
		String path1 = "tbl_mj/table_";
		String path2 = "tbl_mj/eye_table_";
		String path3 = "tbl_mj/feng_table_";
		String path4 = "tbl_mj/feng_eye_table_";

		String path5 = "tbl_mj/ph_table_";
		String path6 = "tbl_mj/ph_eye_table_";
		String path7 = "tbl_mj/ph_feng_table_";
		String path8 = "tbl_mj/ph_feng_eye_table_";

		String path9 = "tbl_mj/ewb_table_";
		String path10 = "tbl_mj/ewb_eye_table_";
		String path11 = "tbl_mj/ewb_feng_table_";

		String path12 = "tbl_mj/feng_chi_table_";
		String path13 = "tbl_mj/feng_chi_eye_table_";

		String path14 = "tbl_mj/feng_chi_dfl_table_";
		String path15 = "tbl_mj/feng_chi_dfl_eye_table_";

		String path16 = "tbl_mj/yao_jiu_table_";
		String path17 = "tbl_mj/yao_jiu_eye_table_";

		String path18 = "tbl_mj/ji_xian_table_";
		String path19 = "tbl_mj/ji_xian_eye_table_";

		String path20 = "tbl_mj/feng_chi_hd_table_";
		String path21 = "tbl_mj/feng_chi_hd_eye_table_";

		for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
			table[i].load(path1 + i + ".tbl");
			eye_table[i].load(path2 + i + ".tbl");
			feng_table[i].load(path3 + i + ".tbl");
			feng_eye_table[i].load(path4 + i + ".tbl");

			ph_table[i].load(path5 + i + ".tbl");
			ph_eye_table[i].load(path6 + i + ".tbl");
			ph_feng_table[i].load(path7 + i + ".tbl");
			ph_feng_eye_table[i].load(path8 + i + ".tbl");

			ewb_table[i].load(path9 + i + ".tbl");
			ewb_eye_table[i].load(path10 + i + ".tbl");
			ewb_feng_table[i].load(path11 + i + ".tbl");

			feng_chi_table[i].load(path12 + i + ".tbl");
			feng_chi_eye_table[i].load(path13 + i + ".tbl");

			feng_chi_dfl_table[i].load(path14 + i + ".tbl");
			feng_chi_dfl_eye_table[i].load(path15 + i + ".tbl");

			yao_jiu_table[i].load(path16 + i + ".tbl");
			yao_jiu_eye_table[i].load(path17 + i + ".tbl");

			ji_xian_table[i].load(path18 + i + ".tbl");
			ji_xian_eye_table[i].load(path19 + i + ".tbl");

			feng_chi_hd_table[i].load(path20 + i + ".tbl");
			feng_chi_hd_eye_table[i].load(path21 + i + ".tbl");
		}
	}
}
