import { useNavigate, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { AlertCircle, Home } from 'lucide-react';

interface LocationState {
  errorCode?: string;
  errorMessage?: string;
}

export function ErrorPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState | null;

  const errorCode = state?.errorCode ?? '404';
  const errorMessage = state?.errorMessage ?? 'The page you are looking for does not exist.';

  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <Card className="w-full max-w-lg text-center">
        <CardContent className="pt-8 pb-8">
          <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-[#F87171]/20">
            <AlertCircle className="h-10 w-10 text-[#F87171]" />
          </div>
          <h2 className="text-3xl font-bold text-white">System Error</h2>
          <div className="mt-4 space-y-2">
            <div className="text-sm text-[#94A3B8]">
              <span className="font-medium text-[#CBD5E1]">Error Code: </span>
              <span className="text-[#F87171]">{errorCode}</span>
            </div>
            <p className="text-[#CBD5E1]">{errorMessage}</p>
          </div>
          <div className="mt-8 flex justify-center gap-3">
            <Button onClick={() => navigate('/')}>
              <Home className="mr-2 h-4 w-4" />
              Return to Dashboard
            </Button>
            <Button variant="outline" onClick={() => navigate(-1)}>
              Go Back
            </Button>
          </div>
          <p className="mt-6 text-xs text-[#94A3B8]">Press ENTER to continue</p>
        </CardContent>
      </Card>
    </div>
  );
}
