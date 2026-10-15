import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface ExitDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: () => void
}

export function ExitDialog({ open, onOpenChange, onConfirm }: ExitDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>End your session?</DialogTitle>
          <DialogDescription>
            This is MENMAP option 3 (Exit). You will leave the Portfolio Management System.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Stay
          </Button>
          <Button onClick={onConfirm}>Exit</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
