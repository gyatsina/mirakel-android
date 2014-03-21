package de.azapps.mirakel.model.account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.tools.Log;

public class AccountMirakel extends AccountBase {
	public enum ACCOUNT_TYPES {
		CALDAV, LOCAL, TASKWARRIOR;
		public static ACCOUNT_TYPES getSyncType(final String type) {
			if (type.equals("Taskwarrior")) {
				return TASKWARRIOR;
			} else if (type.equals("CalDav")) {
				return CALDAV;
			} else {
				return LOCAL;
			}
		}

		public static ACCOUNT_TYPES parseAccountType(final String type) {
			if (type.equals(ACCOUNT_TYPE_DAVDROID)) {
				return CALDAV;
			} else if (type.equals(ACCOUNT_TYPE_MIRAKEL)) {
				return TASKWARRIOR;
			} else {
				return LOCAL;
			}
		}

		public static ACCOUNT_TYPES parseInt(final int i) {
			switch (i) {
			case -1:
				return LOCAL;
			case 1:
				return CALDAV;
			case 2:
				return TASKWARRIOR;
			default:
				throw new IllegalArgumentException();
			}

		}

		public static String toName(final ACCOUNT_TYPES type) {
			switch (type) {
			case CALDAV:
				return ACCOUNT_TYPE_DAVDROID;
			case TASKWARRIOR:
				return ACCOUNT_TYPE_MIRAKEL;
			case LOCAL:
			default:
				return null;
			}
		}

		public int toInt() {
			switch (this) {
			case CALDAV:
				return 1;
			case LOCAL:
				return -1;
			case TASKWARRIOR:
				return 2;
			default:
				throw new RuntimeException();
			}
		}

		public String typeName(final Context ctx) {
			switch (this) {
			case CALDAV:
				return ctx.getString(R.string.calDavName);
			case LOCAL:
				return ctx.getString(R.string.local_account);
			case TASKWARRIOR:
				return ctx.getString(R.string.tw_account);
			default:
				return "Unkown account type";
			}
		}
	}

	public static final String ACCOUNT_TYPE_DAVDROID = "bitfire.at.davdroid";

	public static final String ACCOUNT_TYPE_MIRAKEL = "de.azapps.mirakel";
	private static final String[] allColumns = { DatabaseHelper.ID,
			DatabaseHelper.NAME, TYPE, ENABLED };
	private static Context context;
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	public static final String TABLE = "account";

	private static final String TAG = "Account";

	/**
	 * Close the Database–Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	private static AccountMirakel cursorToAccount(final Cursor c) {
		return new AccountMirakel(c.getInt(0), c.getString(1),
				ACCOUNT_TYPES.parseInt(c.getInt(2)), c.getInt(3) == 1);
	}

	private static List<AccountMirakel> cursorToAccountList(final Cursor c) {
		if (c.getCount() > 0) {
			final List<AccountMirakel> accounts = new ArrayList<AccountMirakel>();
			c.moveToFirst();
			while (!c.isAfterLast()) {
				accounts.add(cursorToAccount(c));
				c.moveToNext();
			}
			return accounts;

		}
		return null;
	}

	public static AccountMirakel get(final Account account) {
		final Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME
				+ "='" + account.name + "'", null, null, null, null);
		if (c.getCount() < 1) {
			c.close();
			return null;
		}
		c.moveToFirst();
		final AccountMirakel a = cursorToAccount(c);
		c.close();
		return a;
	}

	public static AccountMirakel get(final int id) {
		final Cursor c = database.query(TABLE, allColumns, DatabaseHelper.ID
				+ " = " + id, null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			final AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		}
		c.close();
		return null;
	}

	public static int countRemoteAccounts() {
		final Cursor c = database.rawQuery("SELECT COUNT(*) from " + TABLE
				+ " WHERE not " + TYPE + "=" + ACCOUNT_TYPES.LOCAL.toInt()
				+ " AND " + ENABLED + "=1", null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			final int ret = c.getInt(0);
			c.close();
			return ret;
		}
		c.close();
		return 0;
	}

	public static List<AccountMirakel> getAll() {
		final Cursor c = database.query(TABLE, allColumns, null, null, null,
				null, null);
		final List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	public static AccountMirakel getByName(final String name) {
		final Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME
				+ "='" + name + "'", null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			final AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		}
		c.close();
		return null;
	}

	public static List<AccountMirakel> getEnabled(final boolean isEnabled) {
		final Cursor c = database.query(TABLE, allColumns, ENABLED + "="
				+ (isEnabled ? 1 : 0), null, null, null, null);
		final List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	public static AccountMirakel getLocal() {
		final Cursor c = database.query(TABLE, allColumns, TYPE + "="
				+ ACCOUNT_TYPES.LOCAL.toInt() + " AND " + ENABLED + "=1", null,
				null, null, null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			final AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		}
		c.close();
		return newAccount(context.getString(R.string.local_account),
				ACCOUNT_TYPES.LOCAL, true);
	}

	public static List<AccountMirakel> getRemote() {
		final Cursor c = database.query(TABLE, allColumns, "not " + TYPE + "="
				+ ACCOUNT_TYPES.LOCAL.toInt() + " AND " + ENABLED + "=1", null,
				null, null, null);
		final List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	public static void init(final Context ctx) {
		AccountMirakel.context = ctx;
		dbHelper = new DatabaseHelper(ctx);
		database = dbHelper.getWritableDatabase();
	}

	public static AccountMirakel newAccount(final String name,
			final ACCOUNT_TYPES type, final boolean enabled) {
		final ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.NAME, name);
		cv.put(TYPE, type.toInt());
		cv.put(ENABLED, enabled);
		final long id = database.insert(TABLE, null, cv);
		final Cursor cursor = database.query(TABLE, allColumns,
				DatabaseHelper.ID + " = " + id, null, null, null, null);
		cursor.moveToFirst();
		final AccountMirakel newAccount = cursorToAccount(cursor);
		cursor.close();
		return newAccount;
	}

	public static void update(final Account[] accounts) {
		final List<AccountMirakel> accountList = AccountMirakel.getAll();
		final int countRemotes = AccountMirakel.countRemoteAccounts();
		final Map<String, AccountMirakel> map = new HashMap<String, AccountMirakel>();
		for (final AccountMirakel a : accountList) {
			map.put(a.getName(), a);
		}
		for (final Account a : accounts) {
			Log.d(TAG, "Accountname: " + a.name + " | TYPE: " + a.type);
			if (a.type.equals(AccountMirakel.ACCOUNT_TYPE_MIRAKEL)
					|| a.type.equals(AccountMirakel.ACCOUNT_TYPE_DAVDROID)) {
				Log.d(TAG, "is supportet Account");
				if (!map.containsKey(a.name)) {
					// Add new account here....
					AccountMirakel.newAccount(a.name,
							ACCOUNT_TYPES.parseAccountType(a.type), true);
				} else {
					// Account exists..
					map.remove(a.name);
				}

			}
		}
		for (final Entry<String, AccountMirakel> el : map.entrySet()) {
			// Remove deleted accounts
			if (el.getValue().getType() != ACCOUNT_TYPES.LOCAL) {
				el.getValue().destroy();
			}
		}
		final int countRemotesNow = AccountMirakel.countRemoteAccounts();

		if (countRemotes == 0 && countRemotesNow == 1) {
			// If we just added our first remote account we want to set it as
			// the default one.
			final List<AccountMirakel> remotes = AccountMirakel.getRemote();

			// This could happen, the operations are not atomar
			if (remotes.size() != 0) {
				final AccountMirakel account = remotes.get(0);
				MirakelModelPreferences.setDefaultAccount(account);
				ListMirakel.setDefaultAccount(account);
			}
		} else if (countRemotes == 1 && countRemotesNow > 1) {
			// If we have now more than one remote account we want to show the
			// account name in the listfragment
			MirakelCommonPreferences.setShowAccountName(true);
		}
	}

	public AccountMirakel(final int id, final String name,
			final ACCOUNT_TYPES type, final boolean enabled) {
		super(id, name, type, enabled);
	}

	public void destroy() {
		if (getType() == ACCOUNT_TYPES.LOCAL) {
			return;
		}
		database.delete(TABLE, DatabaseHelper.ID + "=" + getId(), null);
		final ContentValues cv = new ContentValues();
		cv.put(ListMirakel.ACCOUNT_ID, getLocal().getId());
		database.update(ListMirakel.TABLE, cv, "account_id=" + getId(), null);
		final Account a = getAndroidAccount();
		if (a == null) {
			Log.wtf(TAG, "account not found");
			return;
		}
		AccountManager.get(context).removeAccount(a, null, null);
	}

	public Account getAndroidAccount() {
		final AccountManager am = AccountManager.get(context);
		final Account[] accounts = am.getAccountsByType(ACCOUNT_TYPES
				.toName(getType()));
		for (final Account a : accounts) {
			if (a.name.equals(getName())) {
				return a;
			}
		}
		return null;
	}

	public void save() {
		database.update(TABLE, getContentValues(), DatabaseHelper.ID + "="
				+ getId(), null);
	}

}
