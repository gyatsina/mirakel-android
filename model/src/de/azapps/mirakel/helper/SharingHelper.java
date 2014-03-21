package de.azapps.mirakel.helper;

import android.content.Context;
import android.content.Intent;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

public class SharingHelper {

	/**
	 * Share something
	 * 
	 * @param context
	 * @param subject
	 * @param shareBody
	 */
	static void share(final Context context, final String subject,
			String shareBody) {
		shareBody += "\n\n" + context.getString(R.string.share_footer);
		final Intent sharingIntent = new Intent(
				android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

		final Intent ci = Intent.createChooser(sharingIntent, context
				.getResources().getString(R.string.share_using));
		ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(ci);
	}

	/**
	 * Share a list of Tasks from a List with other apps
	 * 
	 * @param ctx
	 * @param l
	 */
	public static void share(final Context ctx, final ListMirakel l) {
		final String subject = ctx.getString(R.string.share_list_title,
				l.getName(), l.countTasks());
		String body = "";
		for (final Task t : l.tasks()) {
			if (t.isDone()) {
				// body += "* ";
				continue;
			}
			body += "* ";
			body += TaskHelper.getTaskName(ctx, t) + "\n";
		}
		share(ctx, subject, body);
	}

	// Sharing
	/**
	 * Share a Task as text with other apps
	 * 
	 * @param ctx
	 * @param t
	 */
	public static void share(final Context ctx, final Task t) {
		final String subject = TaskHelper.getTaskName(ctx, t);
		share(ctx, subject, t.getContent());
	}

}
