package billedanalyse;

import com.google.zxing.Result;

public interface TagListener
{

	public void onTag(Result result, float orientation);
	
}
