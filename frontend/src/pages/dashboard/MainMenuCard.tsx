import { useId, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import { useShell } from '@/components/layout/shellContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { MENMAP_INVALID_OPTION_MESSAGE, MENMAP_OPTIONS, type MenmapOption } from '@/nav/navigation'

/** The CICS MENMAP screen (INQSET.bms lines 7-19) as a dashboard card. */
export function MainMenuCard() {
  const navigate = useNavigate()
  const { setMessage, requestExit } = useShell()
  const [option, setOption] = useState('')
  const inputId = useId()

  function select(choice: MenmapOption) {
    setMessage(null)
    if (choice.kind === 'exit') requestExit()
    else navigate(choice.path)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const choice = MENMAP_OPTIONS.find((item) => item.option === option.trim())
    if (!choice) {
      setMessage({ text: MENMAP_INVALID_OPTION_MESSAGE, tone: 'error' })
      return
    }
    setOption('')
    select(choice)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Main menu</CardTitle>
        <CardDescription>Select an option, or type its number and press Enter.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <ol className="space-y-1">
          {MENMAP_OPTIONS.map((choice) => (
            <li key={choice.option}>
              <button
                type="button"
                onClick={() => select(choice)}
                className="flex w-full items-center gap-3 rounded-md px-2 py-1.5 text-left text-sm hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <span className="flex h-6 w-6 items-center justify-center rounded bg-secondary font-mono text-xs font-semibold">
                  {choice.option}
                </span>
                {choice.label}
              </button>
            </li>
          ))}
        </ol>
        <form onSubmit={handleSubmit} className="flex items-end gap-2">
          <div className="flex flex-col gap-1">
            <label htmlFor={inputId} className="text-xs font-medium text-muted-foreground">
              Select option
            </label>
            <input
              id={inputId}
              value={option}
              onChange={(event) => setOption(event.target.value.replace(/\D/g, '').slice(0, 1))}
              inputMode="numeric"
              maxLength={1}
              autoComplete="off"
              className="h-9 w-16 rounded-md border border-input bg-background px-3 text-center font-mono text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>
          <Button type="submit" size="sm">
            Go
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
